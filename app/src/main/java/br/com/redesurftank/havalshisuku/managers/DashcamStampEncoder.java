package br.com.redesurftank.havalshisuku.managers;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Burns an overlay into a clip: hardware decode → OpenGL (video frame + overlay bitmap with alpha)
 * → hardware H.264 encode → MP4. Everything stays on the GPU/codec; the CPU only draws the overlay
 * bitmap, and only when {@link Overlay#bitmapFor} returns a new one. Video only, like the saved MP4.
 */
public final class DashcamStampEncoder {
    private static final String TAG = "DashcamStamp";
    private static final long TIMEOUT_US = 10_000L;

    /** Supplies the overlay for a frame; return the same bitmap to keep the current texture. */
    public interface Overlay {
        Bitmap bitmapFor(long presentationUs, int width, int height);
    }

    public interface Progress {
        /** 0..1, or false to cancel. */
        boolean onProgress(float fraction);
    }

    /** Re-encodes [input] into [output] with the overlay. Returns null on success, else an error code. */
    public static String encode(File input, File output, Overlay overlay, Progress progress) {
        MediaExtractor extractor = new MediaExtractor();
        MediaCodec decoder = null;
        MediaCodec encoder = null;
        MediaMuxer muxer = null;
        Gl gl = null;
        HandlerThread frameThread = new HandlerThread("StampFrames");
        frameThread.start();
        boolean muxerStarted = false;
        try {
            extractor.setDataSource(input.getAbsolutePath());
            int track = -1;
            MediaFormat inFormat = null;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat f = extractor.getTrackFormat(i);
                String mime = f.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("video/")) {
                    track = i;
                    inFormat = f;
                    break;
                }
            }
            if (track < 0) return "no_video";
            extractor.selectTrack(track);
            int srcWidth = inFormat.getInteger(MediaFormat.KEY_WIDTH);
            int srcHeight = inFormat.getInteger(MediaFormat.KEY_HEIGHT);
            // Field 2026-11-25: the head unit's H.264 encoder fails on 4K surface input ("Color
            // Conversion failed", OMX_ErrorHardware). The stamped copy is made at most 1080p; the
            // decoded frame is scaled when drawn and the stamp is laid out for the output size.
            int[] outSize = outputSize(srcWidth, srcHeight);
            int width = outSize[0];
            int height = outSize[1];
            long durationUs = inFormat.containsKey(MediaFormat.KEY_DURATION) ? inFormat.getLong(MediaFormat.KEY_DURATION) : 60_000_000L;
            int fps = inFormat.containsKey(MediaFormat.KEY_FRAME_RATE) ? inFormat.getInteger(MediaFormat.KEY_FRAME_RATE) : 30;

            MediaFormat outFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
            outFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            outFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrateFor(width, height));
            outFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
            outFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            encoder.configure(outFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            Surface encoderInput = encoder.createInputSurface();
            encoder.start();

            gl = new Gl(encoderInput, width, height, new Handler(frameThread.getLooper()));
            decoder = MediaCodec.createDecoderByType(inFormat.getString(MediaFormat.KEY_MIME));
            decoder.configure(inFormat, gl.decoderSurface, null, 0);
            decoder.start();

            muxer = new MediaMuxer(output.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            int muxTrack = -1;
            MediaCodec.BufferInfo decInfo = new MediaCodec.BufferInfo();
            MediaCodec.BufferInfo encInfo = new MediaCodec.BufferInfo();
            boolean inputDone = false;
            boolean decodeDone = false;
            boolean encodeDone = false;
            Bitmap current = null;
            long firstPts = -1;

            while (!encodeDone) {
                if (!inputDone) {
                    int in = decoder.dequeueInputBuffer(TIMEOUT_US);
                    if (in >= 0) {
                        ByteBuffer buf = decoder.getInputBuffer(in);
                        int size = buf == null ? -1 : extractor.readSampleData(buf, 0);
                        if (size < 0) {
                            decoder.queueInputBuffer(in, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputDone = true;
                        } else {
                            decoder.queueInputBuffer(in, 0, size, extractor.getSampleTime(), 0);
                            extractor.advance();
                        }
                    }
                }
                // Drain the encoder first so it never stalls the decoder.
                while (true) {
                    int out = encoder.dequeueOutputBuffer(encInfo, 0);
                    if (out == MediaCodec.INFO_TRY_AGAIN_LATER) break;
                    if (out == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        muxTrack = muxer.addTrack(encoder.getOutputFormat());
                        muxer.start();
                        muxerStarted = true;
                        continue;
                    }
                    if (out < 0) continue;
                    ByteBuffer data = encoder.getOutputBuffer(out);
                    if ((encInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) encInfo.size = 0;
                    if (encInfo.size > 0 && muxerStarted && data != null) {
                        data.position(encInfo.offset);
                        data.limit(encInfo.offset + encInfo.size);
                        muxer.writeSampleData(muxTrack, data, encInfo);
                    }
                    encoder.releaseOutputBuffer(out, false);
                    if ((encInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        encodeDone = true;
                        break;
                    }
                }
                if (!decodeDone && !encodeDone) {
                    int out = decoder.dequeueOutputBuffer(decInfo, TIMEOUT_US);
                    if (out >= 0) {
                        boolean eos = (decInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                        boolean render = decInfo.size > 0;
                        decoder.releaseOutputBuffer(out, render);
                        if (render) {
                            if (!gl.awaitFrame()) return "frame_timeout";
                            if (firstPts < 0) firstPts = decInfo.presentationTimeUs;
                            long pts = decInfo.presentationTimeUs - firstPts;
                            Bitmap next = overlay.bitmapFor(pts, width, height);
                            if (next != current && next != null) {
                                gl.uploadOverlay(next);
                                current = next;
                            }
                            gl.draw(current != null);
                            gl.present(pts * 1000L);
                            if (progress != null && !progress.onProgress(Math.min(1f, pts / (float) Math.max(1L, durationUs)))) {
                                return "cancelled";
                            }
                        }
                        if (eos) {
                            decodeDone = true;
                            encoder.signalEndOfInputStream();
                        }
                    }
                }
            }
            return muxerStarted ? null : "empty_output";
        } catch (IOException | RuntimeException e) {
            Log.w(TAG, "stamp encode failed", e);
            return "encode_" + e.getClass().getSimpleName();
        } finally {
            try { if (decoder != null) { decoder.stop(); decoder.release(); } } catch (RuntimeException ignored) {}
            try { if (encoder != null) { encoder.stop(); encoder.release(); } } catch (RuntimeException ignored) {}
            try { if (muxer != null) { if (muxerStarted) muxer.stop(); muxer.release(); } } catch (RuntimeException ignored) {}
            if (gl != null) gl.release();
            extractor.release();
            frameThread.quitSafely();
        }
    }

    /** Roughly the camera's own quality: ~12 Mbit/s at 1080p, scaled by area. */
    static final int MAX_OUT_WIDTH = 1920;
    static final int MAX_OUT_HEIGHT = 1080;

    /** Source size scaled down (never up) to fit 1920x1080, aspect kept, even dimensions. */
    static int[] outputSize(int width, int height) {
        if (width <= MAX_OUT_WIDTH && height <= MAX_OUT_HEIGHT) return new int[]{width & ~1, height & ~1};
        double k = Math.min(MAX_OUT_WIDTH / (double) width, MAX_OUT_HEIGHT / (double) height);
        int w = (int) Math.round(width * k) & ~1;
        int h = (int) Math.round(height * k) & ~1;
        return new int[]{Math.max(2, w), Math.max(2, h)};
    }

    static int bitrateFor(int width, int height) {
        long px = (long) width * height;
        long bps = px * 12_000_000L / (1920L * 1080L);
        return (int) Math.max(4_000_000L, Math.min(40_000_000L, bps));
    }

    /** EGL on the encoder's input surface, an OES texture for decoded frames, a 2D texture for the overlay. */
    private static final class Gl {
        private static final int EGL_RECORDABLE_ANDROID = 0x3142;
        private static final String VS =
                "attribute vec4 aPos; attribute vec4 aTex; uniform mat4 uTexM; varying vec2 vTex;"
                        + "void main(){ gl_Position = aPos; vTex = (uTexM * aTex).xy; }";
        private static final String FS_OES =
                "#extension GL_OES_EGL_image_external : require\nprecision mediump float;"
                        + "varying vec2 vTex; uniform samplerExternalOES uTex;"
                        + "void main(){ gl_FragColor = texture2D(uTex, vTex); }";
        private static final String FS_2D =
                "precision mediump float; varying vec2 vTex; uniform sampler2D uTex;"
                        + "void main(){ gl_FragColor = texture2D(uTex, vTex); }";
        private static final float[] QUAD = {-1, -1, 1, -1, -1, 1, 1, 1};
        private static final float[] TEX = {0, 0, 1, 0, 0, 1, 1, 1};
        /** Bitmaps are top-down; GL textures bottom-up. */
        private static final float[] TEX_FLIP = {0, 1, 1, 1, 0, 0, 1, 0};

        private final EGLDisplay display;
        private final EGLContext context;
        private final EGLSurface surface;
        private final int width;
        private final int height;
        private final int oesTex;
        private final int overlayTex;
        private final int oesProgram;
        private final int texProgram;
        private final SurfaceTexture surfaceTexture;
        final Surface decoderSurface;
        private final Object frameLock = new Object();
        private boolean frameAvailable;
        private final float[] texMatrix = new float[16];
        private final float[] identity = {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
        private final FloatBuffer quad = buffer(QUAD);
        private final FloatBuffer tex = buffer(TEX);
        private final FloatBuffer texFlip = buffer(TEX_FLIP);

        Gl(Surface encoderInput, int width, int height, Handler frameHandler) {
            this.width = width;
            this.height = height;
            display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
            int[] version = new int[2];
            if (!EGL14.eglInitialize(display, version, 0, version, 1)) throw new IllegalStateException("egl_init");
            int[] attribs = {
                    EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, EGL_RECORDABLE_ANDROID, 1, EGL14.EGL_NONE};
            EGLConfig[] configs = new EGLConfig[1];
            int[] num = new int[1];
            if (!EGL14.eglChooseConfig(display, attribs, 0, configs, 0, 1, num, 0) || num[0] == 0) {
                throw new IllegalStateException("egl_config");
            }
            context = EGL14.eglCreateContext(display, configs[0], EGL14.EGL_NO_CONTEXT,
                    new int[]{EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE}, 0);
            surface = EGL14.eglCreateWindowSurface(display, configs[0], encoderInput, new int[]{EGL14.EGL_NONE}, 0);
            if (!EGL14.eglMakeCurrent(display, surface, surface, context)) throw new IllegalStateException("egl_current");

            int[] tex = new int[2];
            GLES20.glGenTextures(2, tex, 0);
            oesTex = tex[0];
            overlayTex = tex[1];
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTex);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, overlayTex);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            oesProgram = program(VS, FS_OES);
            texProgram = program(VS, FS_2D);

            surfaceTexture = new SurfaceTexture(oesTex);
            surfaceTexture.setOnFrameAvailableListener(st -> {
                synchronized (frameLock) {
                    frameAvailable = true;
                    frameLock.notifyAll();
                }
            }, frameHandler);
            decoderSurface = new Surface(surfaceTexture);
        }

        boolean awaitFrame() {
            synchronized (frameLock) {
                long deadline = System.currentTimeMillis() + 2_500L;
                while (!frameAvailable) {
                    long left = deadline - System.currentTimeMillis();
                    if (left <= 0) return false;
                    try {
                        frameLock.wait(left);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
                frameAvailable = false;
            }
            surfaceTexture.updateTexImage();
            surfaceTexture.getTransformMatrix(texMatrix);
            return true;
        }

        void uploadOverlay(Bitmap bitmap) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, overlayTex);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        }

        void draw(boolean withOverlay) {
            GLES20.glViewport(0, 0, width, height);
            GLES20.glDisable(GLES20.GL_BLEND);
            quadDraw(oesProgram, GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTex, tex, texMatrix);
            if (withOverlay) {
                GLES20.glEnable(GLES20.GL_BLEND);
                // Canvas bitmaps are premultiplied.
                GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
                quadDraw(texProgram, GLES20.GL_TEXTURE_2D, overlayTex, texFlip, identity);
            }
        }

        void present(long ptsNs) {
            EGLExt.eglPresentationTimeANDROID(display, surface, ptsNs);
            EGL14.eglSwapBuffers(display, surface);
        }

        private void quadDraw(int program, int target, int texture, FloatBuffer texCoords, float[] m) {
            GLES20.glUseProgram(program);
            int aPos = GLES20.glGetAttribLocation(program, "aPos");
            int aTex = GLES20.glGetAttribLocation(program, "aTex");
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(target, texture);
            GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "uTex"), 0);
            GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(program, "uTexM"), 1, false, m, 0);
            quad.position(0);
            texCoords.position(0);
            GLES20.glEnableVertexAttribArray(aPos);
            GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, 0, quad);
            // aTex is vec4: z=0, w=1 come from the defaults for missing components.
            GLES20.glEnableVertexAttribArray(aTex);
            GLES20.glVertexAttribPointer(aTex, 2, GLES20.GL_FLOAT, false, 0, texCoords);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            GLES20.glDisableVertexAttribArray(aPos);
            GLES20.glDisableVertexAttribArray(aTex);
        }

        void release() {
            try {
                decoderSurface.release();
                surfaceTexture.release();
                EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
                EGL14.eglDestroySurface(display, surface);
                EGL14.eglDestroyContext(display, context);
                EGL14.eglReleaseThread();
                EGL14.eglTerminate(display);
            } catch (RuntimeException ignored) {
            }
        }

        private static FloatBuffer buffer(float[] values) {
            FloatBuffer b = ByteBuffer.allocateDirect(values.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            b.put(values).position(0);
            return b;
        }

        private static int program(String vs, String fs) {
            int v = shader(GLES20.GL_VERTEX_SHADER, vs);
            int f = shader(GLES20.GL_FRAGMENT_SHADER, fs);
            int p = GLES20.glCreateProgram();
            GLES20.glAttachShader(p, v);
            GLES20.glAttachShader(p, f);
            GLES20.glLinkProgram(p);
            int[] ok = new int[1];
            GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, ok, 0);
            if (ok[0] == 0) throw new IllegalStateException("gl_link " + GLES20.glGetProgramInfoLog(p));
            return p;
        }

        private static int shader(int type, String src) {
            int s = GLES20.glCreateShader(type);
            GLES20.glShaderSource(s, src);
            GLES20.glCompileShader(s);
            int[] ok = new int[1];
            GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, ok, 0);
            if (ok[0] == 0) throw new IllegalStateException("gl_compile " + GLES20.glGetShaderInfoLog(s));
            return s;
        }
    }
}
