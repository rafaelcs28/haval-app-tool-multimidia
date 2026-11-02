#pragma once

#include <atomic>
#include <cstddef>
#include <cstdint>
#include <ctime>

#if defined(__ANDROID__)
#include <android/log.h>
#endif

namespace carplay_experiment {

/** Bounded, payload-free milestones for the explicit clean-map field experiment. */
class PipelineTrace final {
public:
    static void reset(std::uint64_t generation, std::uint32_t width, std::uint32_t height) noexcept {
        generation_.store(generation);
        getterEntry_.store(0); getterRuntime_.store(0); secondarySessions_.store(0);
        secondaryInitializes_.store(0); packets_.store(0); bytes_.store(0); idrs_.store(0);
        packetRetries_.store(0); packetFailures_.store(0); inputs_.store(0); retries_.store(0);
        outputs_.store(0); faults_.store(0);
        event("activation_start", 0, width, height);
    }

    static void getterEntry() noexcept { counted("getter_entry", getterEntry_, 0, 0); }
    static void getterRuntime(std::int32_t status, bool published) noexcept {
        counted("getter_runtime", getterRuntime_, status, published ? 1U : 0U);
    }
    static void session(std::uint32_t type, std::int32_t status) noexcept {
        if (type == 111U) counted("session_111", secondarySessions_, status, type);
    }
    static void initialized(bool secondary, std::int32_t status) noexcept {
        if (secondary) counted("initialize_111", secondaryInitializes_, status, 0);
    }
    static void dimensions(std::uint32_t width, std::uint32_t height, std::int32_t status) noexcept {
        event("dimensions", status, width, height);
    }
    static void avcc(std::size_t length, std::int32_t status) noexcept {
        event("avcc", status, static_cast<std::uint64_t>(length), 0);
    }
    static void decoderConfigured(std::uint32_t width, std::uint32_t height, bool success) noexcept {
        event("codec_configure", success ? 0 : -1, width, height);
    }
    static void packet(std::size_t length, bool idr, std::int32_t status) noexcept {
        const auto count = packets_.fetch_add(1) + 1;
        const auto total = bytes_.fetch_add(static_cast<std::uint64_t>(length)) + length;
        if (idr) idrs_.fetch_add(1);
        std::uint64_t anomalyCount = 0;
        if (status == 1) anomalyCount = packetRetries_.fetch_add(1) + 1;
        else if (status != 0) anomalyCount = packetFailures_.fetch_add(1) + 1;
        if (reportCount(count) || (anomalyCount != 0 && reportCount(anomalyCount))) {
            event(idr ? "packet_idr" : "packet", status, count, total);
        }
    }
    static void decoderInput(std::int32_t status) noexcept {
        if (status == 0) counted("codec_input", inputs_, status, 0);
        else if (status == 1) counted("codec_retry", retries_, status, 0);
        else { faults_.fetch_add(1); event("codec_input_failure", status, inputs_.load(), retries_.load()); }
    }
    static void outputFormat(std::int32_t width, std::int32_t height, bool valid) noexcept {
        event("codec_format", valid ? 0 : -1,
            width < 0 ? 0U : static_cast<std::uint64_t>(width),
            height < 0 ? 0U : static_cast<std::uint64_t>(height));
    }
    static void outputRendered() noexcept { counted("codec_render", outputs_, 0, 0); }
    static void fault(const char* stage, std::int32_t status) noexcept {
        const auto count = faults_.fetch_add(1) + 1;
        event(stage, status, count, 0);
    }
    static void summary(const char* stage) noexcept {
        event(stage, 0, packets_.load(), outputs_.load());
        event("summary_io", 0, inputs_.load(), retries_.load());
        event("summary_idr_faults", 0, idrs_.load(), faults_.load());
    }

private:
    static bool reportCount(std::uint64_t value) noexcept {
        return value == 1 || (value & (value - 1)) == 0;
    }
    static void counted(const char* name, std::atomic<std::uint64_t>& counter,
                        std::int32_t status, std::uint64_t auxiliary) noexcept {
        const auto count = counter.fetch_add(1) + 1;
        if (reportCount(count)) event(name, status, count, auxiliary);
    }
    static std::uint64_t bootMilliseconds() noexcept {
        timespec value{};
        if (clock_gettime(CLOCK_BOOTTIME, &value) != 0 || value.tv_sec < 0 || value.tv_nsec < 0) return 0;
        return static_cast<std::uint64_t>(value.tv_sec) * 1000U +
            static_cast<std::uint64_t>(value.tv_nsec) / 1000000U;
    }
    static void event(const char* name, std::int32_t status,
                      std::uint64_t value, std::uint64_t auxiliary) noexcept {
#if defined(__ANDROID__)
        __android_log_print(ANDROID_LOG_WARN, "ImpulseCPMap",
            "boot_ms=%llu gen=%llu event=%s status=%d value=%llu aux=%llu",
            static_cast<unsigned long long>(bootMilliseconds()),
            static_cast<unsigned long long>(generation_.load()), name, status,
            static_cast<unsigned long long>(value), static_cast<unsigned long long>(auxiliary));
#else
        (void)name; (void)status; (void)value; (void)auxiliary;
#endif
    }

    inline static std::atomic<std::uint64_t> generation_{0};
    inline static std::atomic<std::uint64_t> getterEntry_{0};
    inline static std::atomic<std::uint64_t> getterRuntime_{0};
    inline static std::atomic<std::uint64_t> secondarySessions_{0};
    inline static std::atomic<std::uint64_t> secondaryInitializes_{0};
    inline static std::atomic<std::uint64_t> packets_{0};
    inline static std::atomic<std::uint64_t> bytes_{0};
    inline static std::atomic<std::uint64_t> idrs_{0};
    inline static std::atomic<std::uint64_t> packetRetries_{0};
    inline static std::atomic<std::uint64_t> packetFailures_{0};
    inline static std::atomic<std::uint64_t> inputs_{0};
    inline static std::atomic<std::uint64_t> retries_{0};
    inline static std::atomic<std::uint64_t> outputs_{0};
    inline static std::atomic<std::uint64_t> faults_{0};
};

}
