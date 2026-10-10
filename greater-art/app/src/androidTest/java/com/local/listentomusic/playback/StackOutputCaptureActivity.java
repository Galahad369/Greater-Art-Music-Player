package com.local.listentomusic.playback;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.widget.Button;

/** Test-only component. Java avoids relying on Kotlin classes supplied only by the target APK. */
public final class StackOutputCaptureActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (state == null) startActivityForResult(getSystemService(MediaProjectionManager.class).createScreenCaptureIntent(), 19);
    }

    @Override public void onActivityResult(int request, int result, Intent consent) {
        super.onActivityResult(request, result, consent);
        if (request != 19 || result != RESULT_OK || consent == null) { finish(); return; }
        // Start from a foreground interaction, not while Android's app chooser owns the foreground.
        Button button = new Button(this);
        button.setText("Measure Greater Art output");
        button.setOnClickListener(view -> {
            button.setEnabled(false);
            ResultReceiver callback = new ResultReceiver(new Handler(Looper.getMainLooper())) {
                @Override protected void onReceiveResult(int code, Bundle data) {
                    if (code == 0) finish();
                    else { button.setText("Capture could not start; see test log"); button.setEnabled(true); }
                }
            };
            startForegroundService(new Intent(this, StackOutputCaptureService.class)
                .putExtra("projection", consent).putExtra("name", getIntent().getStringExtra("name"))
                .putExtra("callback", callback));
        });
        setContentView(button);
    }
}
