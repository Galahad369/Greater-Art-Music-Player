package com.local.listentomusic.ambient;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import java.io.File;
import java.io.IOException;

/** Non-exported debug host. It is absent from release APKs and normal navigation. */
@UnstableApi
public final class AmbientFrameProbeActivity extends Activity {
    public static final String CAPTURE = "capturePixels";
    public static final String STALE_CONTROL = "staleGlowControl";
    private FrameAmbientView frameView;
    private ExoPlayer player;
    private TextView status;
    private volatile String failure;
    private boolean destroyed;
    private boolean crop;
    private volatile boolean formatAccepted;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xff071016);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            androidx.core.graphics.Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            return insets;
        });
        status = new TextView(this);
        status.setTextColor(0xffe6f4f1);
        status.setPadding(20, 16, 20, 16);
        status.setText("Frame-synchronized Ambient • private SDR prototype\nProduction Ambient is unchanged");
        root.addView(status);
        player = new ExoPlayer.Builder(this).build();
        player.setRepeatMode(Player.REPEAT_MODE_ONE);
        frameView = new FrameAmbientView(this, new FrameAmbientView.Listener() {
            @Override public void surfaceReady(Surface surface) {
                if (!isFinishing() && !isDestroyed()) player.setVideoSurface(surface);
            }
            @Override public void failed(String reason) { fail(reason); }
        }, getIntent().getBooleanExtra(CAPTURE, false), getIntent().getBooleanExtra(STALE_CONTROL, false));
        root.addView(frameView, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout controls = new LinearLayout(this);
        addButton(controls, "Play / pause", () -> { if (player.isPlaying()) player.pause(); else player.play(); });
        addButton(controls, "Fit / Crop", () -> { crop = !crop; frameView.setCrop(crop); });
        addButton(controls, "Rotate", () -> setRequestedOrientation(
                getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                        ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
        root.addView(controls);
        setContentView(root);
        player.addListener(new Player.Listener() {
            @Override public void onPlayerError(PlaybackException error) { fail(error.getErrorCodeName()); }
        });
        player.setVideoFrameMetadataListener((ptsUs, releaseNs, format, mediaFormat) -> {
            if (!FrameAmbientFormatPolicy.accepts(format)) {
                frameView.rejectFormat();
                formatAccepted = false;
                runOnUiThread(() -> fail("Unsupported precision: this prototype requires explicit 8-bit SDR"));
                return;
            }
            formatAccepted = true;
            frameView.sourceFrame(ptsUs, releaseNs, format.width * format.pixelWidthHeightRatio / format.height);
        });
        if (ownedFixture()) {
            player.setMediaItem(MediaItem.fromUri(getIntent().getData()));
            player.prepare();
        } else {
            fail("An owned cache fixture is required; no external URI is accepted");
        }
    }

    private boolean ownedFixture() {
        android.net.Uri uri = getIntent().getData();
        if (uri == null || !"file".equals(uri.getScheme()) || uri.getPath() == null) return false;
        try {
            File root = new File(getCacheDir(), "ambient-frame-probe").getCanonicalFile();
            File candidate = new File(uri.getPath()).getCanonicalFile();
            return candidate.isFile() && candidate.getPath().startsWith(root.getPath() + File.separator);
        } catch (IOException error) { return false; }
    }

    private void addButton(LinearLayout parent, String label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setMinHeight(Math.round(48 * getResources().getDisplayMetrics().density));
        button.setOnClickListener(view -> action.run());
        parent.addView(button, new LinearLayout.LayoutParams(0, -2, 1));
    }

    private void fail(String reason) {
        if (destroyed) return;
        failure = reason;
        player.pause();
        status.setText("Prototype stopped: " + reason + "\nNo lower-quality fallback is used.");
    }

    public ExoPlayer probePlayer() { return player; }
    public FrameAmbientView probeView() { return frameView; }
    public String failure() { return failure != null ? failure : frameView.failure(); }
    public boolean formatAccepted() { return formatAccepted; }

    @Override protected void onResume() { super.onResume(); if (frameView != null) frameView.onResume(); }
    @Override protected void onPause() {
        if (player != null) player.pause();
        if (frameView != null) frameView.onPause();
        super.onPause();
    }
    @Override protected void onDestroy() {
        destroyed = true;
        if (player != null) player.release();
        if (frameView != null) frameView.close();
        super.onDestroy();
    }
}
