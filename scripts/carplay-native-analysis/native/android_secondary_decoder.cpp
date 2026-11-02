#include "android_secondary_decoder.h"
#include "avc_configuration.h"
#include "pipeline_trace.h"

#include <media/NdkMediaFormat.h>
#include <cstring>
#include <limits>
#include <stdexcept>
#include <utility>

namespace carplay_experiment {

AndroidSecondaryDecoder::AndroidSecondaryDecoder(ANativeWindow* independentWindow,
                                               TickConverter tickConverter)
    : window_(independentWindow), tickConverter_(std::move(tickConverter)) {
    if (!window_ || !tickConverter_) {
        throw std::invalid_argument("Independent window and explicit tick conversion required");
    }
    ANativeWindow_acquire(window_);
}

AndroidSecondaryDecoder::~AndroidSecondaryDecoder() {
    stop();
}

bool AndroidSecondaryDecoder::configure(std::uint32_t width, std::uint32_t height,
                                        const std::vector<std::uint8_t>& avcc) {
    if (!window_ || codec_ || !width || !height ||
        width > static_cast<std::uint32_t>(std::numeric_limits<std::int32_t>::max()) ||
        height > static_cast<std::uint32_t>(std::numeric_limits<std::int32_t>::max())) {
        PipelineTrace::decoderConfigured(width, height, false);
        return false;
    }
    const auto configuration = parseAvcConfiguration(avcc, 64 * 1024);
    if (!configuration.valid) {
        PipelineTrace::decoderConfigured(width, height, false);
        return false;
    }
    codec_ = AMediaCodec_createDecoderByType("video/avc");
    if (!codec_) {
        PipelineTrace::decoderConfigured(width, height, false);
        return false;
    }
    std::unique_ptr<AMediaFormat, decltype(&AMediaFormat_delete)> format(
        AMediaFormat_new(), AMediaFormat_delete);
    if (!format) {
        PipelineTrace::decoderConfigured(width, height, false);
        stop();
        return false;
    }
    AMediaFormat_setString(format.get(), AMEDIAFORMAT_KEY_MIME, "video/avc");
    AMediaFormat_setInt32(format.get(), AMEDIAFORMAT_KEY_WIDTH, static_cast<std::int32_t>(width));
    AMediaFormat_setInt32(format.get(), AMEDIAFORMAT_KEY_HEIGHT, static_cast<std::int32_t>(height));
    AMediaFormat_setBuffer(format.get(), "csd-0", configuration.sequenceParameters.data(),
                          configuration.sequenceParameters.size());
    AMediaFormat_setBuffer(format.get(), "csd-1", configuration.pictureParameters.data(),
                          configuration.pictureParameters.size());
    if (AMediaCodec_configure(codec_, format.get(), window_, nullptr, 0) != AMEDIA_OK ||
        AMediaCodec_start(codec_) != AMEDIA_OK) {
        PipelineTrace::decoderConfigured(width, height, false);
        stop();
        return false;
    }
    started_ = true;
    width_ = width;
    height_ = height;
    running_.store(true);
    try {
        outputThread_ = std::thread([this] { drain(); });
    } catch (...) {
        PipelineTrace::decoderConfigured(width, height, false);
        stop();
        return false;
    }
    PipelineTrace::decoderConfigured(width, height, true);
    return true;
}

SecondaryDecoder::SubmitResult AndroidSecondaryDecoder::submit(
    const std::vector<std::uint8_t>& annexB, std::uint64_t ticks) {
    if (!running_.load() || failed_.load() || !codec_ || annexB.empty()) {
        PipelineTrace::decoderInput(-1);
        return SubmitResult::Fatal;
    }
    const auto timestamp = tickConverter_(ticks);
    if (!timestamp || *timestamp < 0 || (hasTimestamp_ && *timestamp < lastTimestamp_)) {
        PipelineTrace::decoderInput(-2);
        return SubmitResult::Fatal;
    }
    const auto index = AMediaCodec_dequeueInputBuffer(codec_, 2000);
    if (index == AMEDIACODEC_INFO_TRY_AGAIN_LATER) {
        PipelineTrace::decoderInput(1);
        return SubmitResult::Retryable;
    }
    if (index < 0) {
        PipelineTrace::decoderInput(-3);
        return SubmitResult::Fatal;
    }
    std::size_t capacity = 0;
    auto* buffer = AMediaCodec_getInputBuffer(codec_, static_cast<std::size_t>(index), &capacity);
    if (!buffer || capacity < annexB.size()) {
        PipelineTrace::decoderInput(-4);
        return SubmitResult::Fatal;
    }
    std::memcpy(buffer, annexB.data(), annexB.size());
    if (AMediaCodec_queueInputBuffer(codec_, static_cast<std::size_t>(index), 0,
                                    annexB.size(), static_cast<std::uint64_t>(*timestamp), 0) != AMEDIA_OK) {
        PipelineTrace::decoderInput(-5);
        return SubmitResult::Fatal;
    }
    hasTimestamp_ = true;
    lastTimestamp_ = *timestamp;
    PipelineTrace::decoderInput(0);
    if (failed_.load()) {
        PipelineTrace::fault("codec_async_failure", -1);
        return SubmitResult::Fatal;
    }
    return SubmitResult::Accepted;
}

void AndroidSecondaryDecoder::drain() noexcept {
    while (running_.load()) {
        AMediaCodecBufferInfo info{};
        const auto index = AMediaCodec_dequeueOutputBuffer(codec_, &info, 10000);
        if (index >= 0) {
            if (AMediaCodec_releaseOutputBuffer(codec_, static_cast<std::size_t>(index), true) != AMEDIA_OK) {
                PipelineTrace::fault("codec_render_failure", -1);
                failed_.store(true);
                running_.store(false);
            } else { PipelineTrace::outputRendered(); }
        } else if (index == AMEDIACODEC_INFO_OUTPUT_FORMAT_CHANGED) {
            std::unique_ptr<AMediaFormat, decltype(&AMediaFormat_delete)> format(
                AMediaCodec_getOutputFormat(codec_), AMediaFormat_delete);
            std::int32_t width = 0;
            std::int32_t height = 0;
            if (!format || !AMediaFormat_getInt32(format.get(), AMEDIAFORMAT_KEY_WIDTH, &width) ||
                !AMediaFormat_getInt32(format.get(), AMEDIAFORMAT_KEY_HEIGHT, &height) ||
                width != static_cast<std::int32_t>(width_) || height != static_cast<std::int32_t>(height_)) {
                failed_.store(true);
                running_.store(false);
            }
            PipelineTrace::outputFormat(width, height,
                format && width == static_cast<std::int32_t>(width_) &&
                height == static_cast<std::int32_t>(height_));
        } else if (index != AMEDIACODEC_INFO_TRY_AGAIN_LATER &&
                   index != AMEDIACODEC_INFO_OUTPUT_BUFFERS_CHANGED) {
            failed_.store(true);
            running_.store(false);
            PipelineTrace::fault("codec_output_failure", static_cast<std::int32_t>(index));
        }
    }
}

void AndroidSecondaryDecoder::stop() noexcept {
    if (!stopReported_.exchange(true)) PipelineTrace::summary("codec_stop");
    running_.store(false);
    if (outputThread_.joinable()) {
        outputThread_.join();
    }
    if (codec_) {
        if (started_) {
            AMediaCodec_stop(codec_);
        }
        AMediaCodec_delete(codec_);
        codec_ = nullptr;
    }
    started_ = false;
    if (window_) {
        ANativeWindow_release(window_);
        window_ = nullptr;
    }
}

}
