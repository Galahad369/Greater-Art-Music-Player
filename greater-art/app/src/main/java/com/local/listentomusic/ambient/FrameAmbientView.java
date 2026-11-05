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
 * Video and lighting are drawn in ONE pass from ONE latched decoder texture.
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

    private static final String VERTEX =
            "attribute vec2 aPosition; varying vec2 vUv;\n" +
            "void main(){vUv=aPosition*.5+.5;gl_Position=vec4(aPosition,0.,1.);}";
    private static final String FRAGMENT =
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision highp float; varying vec2 vUv;\n" +
            "uniform samplerExternalOES uFrame; uniform mat4 uTransform;\n" +
            "uniform vec2 uSize; uniform vec4 uStage; uniform vec4 uVideo;\n" +
            "uniform vec4 uBackground; uniform vec3 uHeldGlow; uniform float uHold; uniform float uLight;\n" +
            "vec3 frame(vec2 uv){return texture2D(uFrame,(uTransform*vec4(clamp(uv,0.,1.),0.,1.)).xy).rgb;}\n" +
            "bool inside(vec2 p,vec4 r){return all(greaterThanEqual(p,r.xy))&&all(lessThan(p,r.xy+r.zw));}\n" +
            "void main(){vec2 p=vUv*uSize;\n" +
            " if(inside(p,uStage)){gl_FragColor=inside(p,uVideo)?vec4(frame((p-uVideo.xy)/uVideo.zw),1.):vec4(0.,0.,0.,1.);return;}\n" +
            " vec2 uv=(p-uBackground.xy)/uBackground.zw;vec2 d=vec2(30.)/uBackground.zw;\n" +
            " vec3 c=frame(uv)*.20;\n" +
            " c+=(frame(uv+vec2(d.x,0.))+frame(uv-vec2(d.x,0.))+frame(uv+vec2(0.,d.y))+frame(uv-vec2(0.,d.y)))*.12;\n" +
            " c+=(frame(uv+d)+frame(uv-d)+frame(uv+vec2(d.x,-d.y))+frame(uv+vec2(-d.x,d.y)))*.08;\n" +
            " c=mix(c,uHeldGlow,uHold);\n" +
            " float edge=exp(-vUv.x*5.)+exp(-(1.-vUv.x)*5.);\n" +
            " float ribbon=exp(-pow((vUv.y-(.21+.065*sin(vUv.x*5.))),2.)*220.);\n" +
            " gl_FragColor=vec4(c*(.30+.16*edge+.13*ribbon)*uLight+vec3(.012),1.);}";

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
    private int program;
    private int frameTexture;
    private int positionLocation;
    private int frameLocation;
    private int transformLocation;
    private int sizeLocation;
    private int stageLocation;
    private int videoLocation;
    private int backgroundLocation;
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
    private float[] background;
    private float[] stageFromTop;
    private float light = 1f;
    private boolean layoutDirty = true;

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
            program = GLES20.glCreateProgram();
            int vertex = shader(GLES20.GL_VERTEX_SHADER, VERTEX);
            int fragment = shader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT);
            GLES20.glAttachShader(program, vertex);
            GLES20.glAttachShader(program, fragment);
            GLES20.glLinkProgram(program);
            int[] status = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0);
            GLES20.glDeleteShader(vertex);
            GLES20.glDeleteShader(fragment);
            if (status[0] == 0) throw new IllegalStateException("GL program link failed");
            positionLocation = GLES20.glGetAttribLocation(program, "aPosition");
            frameLocation = GLES20.glGetUniformLocation(program, "uFrame");
            transformLocation = GLES20.glGetUniformLocation(program, "uTransform");
            sizeLocation = GLES20.glGetUniformLocation(program, "uSize");
            stageLocation = GLES20.glGetUniformLocation(program, "uStage");
            videoLocation = GLES20.glGetUniformLocation(program, "uVideo");
            backgroundLocation = GLES20.glGetUniformLocation(program, "uBackground");
            heldLocation = GLES20.glGetUniformLocation(program, "uHeldGlow");
            holdLocation = GLES20.glGetUniformLocation(program, "uHold");
            lightLocation = GLES20.glGetUniformLocation(program, "uLight");
            int[] names = new int[1];
            GLES20.glGenTextures(1, names, 0);
            frameTexture = names[0];
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, frameTexture);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            SurfaceTexture nextTexture = new SurfaceTexture(frameTexture);
            Surface nextInput = new Surface(nextTexture);
            SurfaceTexture oldTexture = texture;
            Surface oldInput = input;
            texture = nextTexture;
            input = nextInput;
            currentTimestamp = 0;
            hasFrame = false;
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
                background = FrameAmbientGeometry.place(0, 0, width, height, aspect, true);
                layoutDirty = false;
            }
            GLES20.glUseProgram(program);
            GLES20.glEnableVertexAttribArray(positionLocation);
            GLES20.glVertexAttribPointer(positionLocation, 2, GLES20.GL_FLOAT, false, 0, quad);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, frameTexture);
            GLES20.glUniform1i(frameLocation, 0);
            GLES20.glUniformMatrix4fv(transformLocation, 1, false, matrix, 0);
            GLES20.glUniform2f(sizeLocation, width, height);
            GLES20.glUniform4fv(stageLocation, 1, stage, 0);
            GLES20.glUniform4fv(videoLocation, 1, video, 0);
            GLES20.glUniform4fv(backgroundLocation, 1, background, 0);
            GLES20.glUniform3fv(heldLocation, 1, heldGlow, 0);
            GLES20.glUniform1f(holdLocation, staleGlowControl && held ? 1 : 0);
            GLES20.glUniform1f(lightLocation, light);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            GLES20.glDisableVertexAttribArray(positionLocation);
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
