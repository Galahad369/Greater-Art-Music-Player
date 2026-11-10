package com.local.listentomusic.ambient;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;
import androidx.media3.common.C;
import androidx.media3.common.util.UnstableApi;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * SDR compositor shared by Now Playing and the private verification host. The frame-available/
 * external-texture pattern follows the AndroidX Media3 1.11.1 GL demo (Apache-2.0).
 * A tiny light map and the final composition use the same latched decoder texture.
 */
@UnstableApi
public final class FrameAmbientView extends GLSurfaceView implements GLSurfaceView.Renderer {
    public interface Listener {
        void surfaceReady(Surface surface);
        void failed(String reason);
    }

    public static final class Sample {
        public final long textureTimestampNs;
        public final long presentationTimeUs;
        public final long observedNs;
        public final int videoRgb;
        public final int glowRgb;
        public final int width;
        public final int height;
        public final boolean crop;

        Sample(long timestamp, long pts, int video, int glow, int width, int height, boolean crop) {
            textureTimestampNs = timestamp;
            presentationTimeUs = pts;
            observedNs = System.nanoTime();
            videoRgb = video;
            glowRgb = glow;
            this.width = width;
            this.height = height;
            this.crop = crop;
        }
    }

    private static final class FrameProgram {
        final int id;
        final int position;
        final int transform;
        final int size;
        final int stage;
        final int video;

        FrameProgram(String fragmentSource) {
            int vertex = shader(GLES20.GL_VERTEX_SHADER, FrameAmbientShaders.VERTEX);
            int fragment = shader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
            id = GLES20.glCreateProgram();
            GLES20.glAttachShader(id, vertex);
            GLES20.glAttachShader(id, fragment);
            GLES20.glLinkProgram(id);
            GLES20.glDeleteShader(vertex);
            GLES20.glDeleteShader(fragment);
            int[] status = new int[1];
            GLES20.glGetProgramiv(id, GLES20.GL_LINK_STATUS, status, 0);
            if (status[0] == 0) throw new IllegalStateException("GL program link failed");
            position = GLES20.glGetAttribLocation(id, "aPosition");
            transform = GLES20.glGetUniformLocation(id, "uTransform");
            size = GLES20.glGetUniformLocation(id, "uSize");
            stage = GLES20.glGetUniformLocation(id, "uStage");
            video = GLES20.glGetUniformLocation(id, "uVideo");
            GLES20.glUseProgram(id);
            GLES20.glUniform1i(GLES20.glGetUniformLocation(id, "uFrame"), 0);
        }
    }

    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicBoolean available = new AtomicBoolean();
    private final FrameAmbientFrameTimes frameTimes = new FrameAmbientFrameTimes();
    private final ArrayDeque<Sample> samples = new ArrayDeque<>();
    private final boolean diagnose;
    private final boolean staleGlowControl;
    private final boolean presentation;
    private final FloatBuffer quad = ByteBuffer.allocateDirect(8 * Float.BYTES)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
    private final ByteBuffer pixel = ByteBuffer.allocateDirect(4);
    private final float[] matrix = new float[16];
    private final float[] heldGlow = new float[3];
    private volatile float aspect = 16f / 9f;
    private volatile boolean crop;
    private volatile boolean closed;
    private volatile boolean formatAllowed;
    private volatile String failure;
    private volatile int renderedFrames;
    private volatile long drawnViewport;
    private volatile String rendererName = "not-created";
    private SurfaceTexture texture;
    private Surface input;
    private FrameProgram composite;
    private FrameProgram analysis;
    private int frameTexture;
    private int beamTexture;
    private int beamFramebuffer;
    private int heldLocation;
    private int holdLocation;
    private int lightLocation;
    private int width;
    private int height;
    private long currentTimestamp;
    private long currentPts = C.TIME_UNSET;
    private boolean hasFrame;
    private boolean held;
    private volatile float[] stage;
    private volatile float[] video;
    private float[] stageFromTop;
    private float light = 1f;
    private boolean layoutDirty = true;
    private boolean beamsDirty = true;

    public FrameAmbientView(Context context, Listener listener, boolean diagnose, boolean staleGlowControl) {
        this(context, listener, diagnose, staleGlowControl, false);
    }

    public FrameAmbientView(Context context, Listener listener) {
        this(context, listener, false, false, true);
    }

    private FrameAmbientView(Context context, Listener listener, boolean diagnose,
            boolean staleGlowControl, boolean presentation) {
        super(context);
        this.listener = listener;
        this.diagnose = diagnose;
        this.staleGlowControl = staleGlowControl;
        this.presentation = presentation;
        quad.put(new float[] {-1, -1, 1, -1, -1, 1, 1, 1}).position(0);
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        setPreserveEGLContextOnPause(true);
        setRenderer(this);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    public void sourceFrame(long ptsUs, long releaseTimeNs, float sourceAspect) {
        if (closed || failure != null) return;
        frameTimes.record(releaseTimeNs, ptsUs);
        formatAllowed = true;
        if (sourceAspect > 0 && Float.isFinite(sourceAspect) && aspect != sourceAspect) {
            aspect = sourceAspect;
            queueEvent(() -> layoutDirty = true);
        }
    }

    public void rejectFormat() { formatAllowed = false; requestRender(); }

    public void refusePresentation() {
        if (closed || failure != null) return;
        formatAllowed = false;
        fail("Video format requires native playback");
    }

    public void setStage(int left, int top, int width, int height, float intensity) {
        queueEvent(() -> {
            stageFromTop = new float[] {left, top, width, height};
            light = intensity;
            layoutDirty = true;
        });
        requestRender();
    }

    public void setCrop(boolean value) {
        crop = value;
        queueEvent(() -> layoutDirty = true);
        requestRender();
    }

    public synchronized List<Sample> snapshot() { return new ArrayList<>(samples); }
    public String failure() { return failure; }
    public int renderedFrames() { return renderedFrames; }
    public String rendererName() { return rendererName; }
    public boolean hasDrawnViewport(int viewWidth, int viewHeight) {
        return drawnViewport == ((long) viewWidth << 32 | (viewHeight & 0xffffffffL));
    }
    public float[] stageRectangle() { float[] value = stage; return value == null ? null : value.clone(); }
    public float[] videoRectangle() { float[] value = video; return value == null ? null : value.clone(); }

    /** Call after releasing the decoder, while the Activity owns this view. */
    public void close() {
        closed = true;
        onPause();
        if (input != null) { input.release(); input = null; }
        if (texture != null) { texture.release(); texture = null; }
    }

    @Override public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        if (closed) return;
        try {
            rendererName = GLES20.glGetString(GLES20.GL_RENDERER);
            analysis = new FrameProgram(FrameAmbientShaders.ANALYZE);
            composite = new FrameProgram(FrameAmbientShaders.COMPOSITE);
            GLES20.glUniform1i(GLES20.glGetUniformLocation(composite.id, "uBeams"), 1);
            heldLocation = GLES20.glGetUniformLocation(composite.id, "uHeldGlow");
            holdLocation = GLES20.glGetUniformLocation(composite.id, "uHold");
            lightLocation = GLES20.glGetUniformLocation(composite.id, "uLight");
            int[] names = new int[1];
            createBeamMap(names);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glGenTextures(1, names, 0);
            frameTexture = names[0];
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, frameTexture);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            if (GLES20.glGetError() != GLES20.GL_NO_ERROR) throw new IllegalStateException("Ambient GL setup failed");
            SurfaceTexture nextTexture = new SurfaceTexture(frameTexture);
            Surface nextInput = new Surface(nextTexture);
            SurfaceTexture oldTexture = texture;
            Surface oldInput = input;
            texture = nextTexture;
            input = nextInput;
            currentTimestamp = 0;
            hasFrame = false;
            beamsDirty = true;
            currentPts = C.TIME_UNSET;
            available.set(false);
            held = false;
            nextTexture.setOnFrameAvailableListener(source -> {
                if (!closed && source == texture) { available.set(true); requestRender(); }
            }, main);
            main.post(() -> {
                if (!closed && input == nextInput) listener.surfaceReady(nextInput);
                if (oldInput != null) oldInput.release();
                if (oldTexture != null) oldTexture.release();
            });
        } catch (RuntimeException error) { fail(error.getClass().getSimpleName()); }
    }

    @Override public void onSurfaceChanged(GL10 unused, int width, int height) {
        this.width = width;
        this.height = height;
        layoutDirty = true;
    }

    @Override public void onDrawFrame(GL10 unused) {
        if (closed || failure != null || width <= 0 || height <= 0) return;
        try {
            GLES20.glViewport(0, 0, width, height);
            GLES20.glClearColor(0, 0, 0, 1);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            // Refuse unsupported precision before latching/drawing even one frame.
            if (!formatAllowed) return;
            boolean fresh = false;
            if (available.getAndSet(false)) {
                texture.updateTexImage();
                long nextTimestamp = texture.getTimestamp();
                fresh = !hasFrame || nextTimestamp != currentTimestamp;
                if (fresh) {
                    currentPts = frameTimes.lookup(nextTimestamp);
                    // A newer HDR/unknown frame can reach the surface before the
                    // controller's track update. Only draw a confirmed SDR buffer.
                    if (presentation && (!formatAllowed || currentPts == C.TIME_UNSET)) {
                        long latest = frameTimes.latest();
                        fail("No matching SDR frame metadata; deltaNs=" +
                                (latest == C.TIME_UNSET ? "none" : Long.toString(nextTimestamp - latest)));
                        return;
                    }
                }
                currentTimestamp = nextTimestamp;
                texture.getTransformMatrix(matrix);
                hasFrame = true;
                beamsDirty = true;
            }
            if (!hasFrame) return;
            if (layoutDirty) {
                if (presentation) {
                    if (stageFromTop == null || stageFromTop[2] <= 0 || stageFromTop[3] <= 0) return;
                    stage = new float[] {stageFromTop[0], height - stageFromTop[1] - stageFromTop[3],
                            stageFromTop[2], stageFromTop[3]};
                } else {
                    stage = new float[] {0, height * .47f, width, height * .49f};
                }
                video = FrameAmbientGeometry.place(stage[0], stage[1], stage[2], stage[3], aspect, crop);
                layoutDirty = false;
                beamsDirty = true;
            }
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, frameTexture);
            if (light > 0f && beamsDirty) {
                drawBeamMap();
                beamsDirty = false;
            }
            useFrameProgram(composite);
            GLES20.glUniform3fv(heldLocation, 1, heldGlow, 0);
            GLES20.glUniform1f(holdLocation, staleGlowControl && held ? 1 : 0);
            GLES20.glUniform1f(lightLocation, light);
            drawQuad(composite);
            if (GLES20.glGetError() != GLES20.GL_NO_ERROR) throw new IllegalStateException("GL draw failed");
            // Publish only after drawing. Android layout can resize before this GL thread.
            drawnViewport = (long) width << 32 | (height & 0xffffffffL);
            if (fresh) {
                renderedFrames++;
                if (diagnose) {
                    int foreground = readPixel(width / 2, Math.round(stage[1] + stage[3] / 2));
                    int glow = readPixel(width / 2, height / 5);
                    synchronized (this) {
                        samples.addLast(new Sample(currentTimestamp, currentPts, foreground, glow, width, height, crop));
                        while (samples.size() > 300) samples.removeFirst();
                    }
                    if (!held && FrameAmbientGeometry.dominant(foreground) >= 0) {
                        heldGlow[0] = ((foreground >>> 16) & 255) / 255f;
                        heldGlow[1] = ((foreground >>> 8) & 255) / 255f;
                        heldGlow[2] = (foreground & 255) / 255f;
                        held = true;
                    }
                }
            }
        } catch (RuntimeException error) { fail(error.getClass().getSimpleName()); }
    }

    private void createBeamMap(int[] names) {
        GLES20.glGenTextures(1, names, 0);
        beamTexture = names[0];
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, beamTexture);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, FrameAmbientShaders.BEAM_COLUMNS,
                FrameAmbientShaders.BEAM_EDGES, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glGenFramebuffers(1, names, 0);
        beamFramebuffer = names[0];
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, beamFramebuffer);
        try {
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                    GLES20.GL_TEXTURE_2D, beamTexture, 0);
            if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE) {
                throw new IllegalStateException("Ambient beam map is incomplete");
            }
        } finally {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        }
    }

    private void drawBeamMap() {
        // Do not latch the SurfaceTexture here. The color and direction must
        // correspond to the exact same decoder buffer as the foreground draw.
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, beamFramebuffer);
        GLES20.glViewport(0, 0, FrameAmbientShaders.BEAM_COLUMNS, FrameAmbientShaders.BEAM_EDGES);
        GLES20.glDisable(GLES20.GL_DITHER); // The RGBA bytes encode coordinates, not display colors.
        try {
            useFrameProgram(analysis);
            drawQuad(analysis);
        } finally {
            GLES20.glEnable(GLES20.GL_DITHER); // Restore the default for the unchanged video draw.
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            GLES20.glViewport(0, 0, width, height);
        }
    }

    private void useFrameProgram(FrameProgram pass) {
        GLES20.glUseProgram(pass.id);
        GLES20.glUniformMatrix4fv(pass.transform, 1, false, matrix, 0);
        GLES20.glUniform2f(pass.size, width, height);
        GLES20.glUniform4fv(pass.stage, 1, stage, 0);
        GLES20.glUniform4fv(pass.video, 1, video, 0);
    }

    private void drawQuad(FrameProgram pass) {
        GLES20.glEnableVertexAttribArray(pass.position);
        GLES20.glVertexAttribPointer(pass.position, 2, GLES20.GL_FLOAT, false, 0, quad);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(pass.position);
    }

    private int readPixel(int x, int y) {
        pixel.position(0);
        GLES20.glReadPixels(x, y, 1, 1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixel);
        return ((pixel.get(0) & 255) << 16) | ((pixel.get(1) & 255) << 8) | (pixel.get(2) & 255);
    }

    private static int shader(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] status = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (status[0] == 0) {
            GLES20.glDeleteShader(shader);
            throw new IllegalStateException("GL shader compile failed");
        }
        return shader;
    }

    private void fail(String reason) {
        failure = reason;
        main.post(() -> listener.failed(reason));
    }
}
