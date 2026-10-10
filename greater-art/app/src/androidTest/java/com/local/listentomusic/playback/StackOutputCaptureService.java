package com.local.listentomusic.playback;

import android.app.*;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.*;
import android.media.projection.*;
import android.os.*;
import android.util.Log;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicBoolean;

/** Separate test APK: explicit app-only Android consent and fixed target UID. Never microphone input. */
public final class StackOutputCaptureService extends Service {
    private final AtomicBoolean active = new AtomicBoolean(false);
    private MediaProjection projection;
    private volatile AudioRecord recorder;
    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (!active.compareAndSet(false, true)) return START_NOT_STICKY;
        ResultReceiver callback = intent.getParcelableExtra("callback");
        try {
            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED)
                throw new SecurityException("The separate test APK needs explicit audio-capture permission");
            NotificationManager notifications = getSystemService(NotificationManager.class);
            notifications.createNotificationChannel(new NotificationChannel("stack-probe", "Stack output test", NotificationManager.IMPORTANCE_LOW));
            startForeground(19, new Notification.Builder(this, "stack-probe")
                .setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("Greater Art output test")
                .setContentText("Only Greater Art playback, 70 seconds; no microphone").build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
            Intent consent = intent.getParcelableExtra("projection");
            if (consent == null) throw new IllegalArgumentException("Missing explicit consent");
            projection = getSystemService(MediaProjectionManager.class).getMediaProjection(Activity.RESULT_OK, consent);
            if (projection == null) throw new IllegalStateException("No projection token");
            projection.registerCallback(new MediaProjection.Callback() {
                @Override public void onStop() { active.set(false); stopRecorder(); }
            }, new Handler(Looper.getMainLooper()));
            int target = getPackageManager().getApplicationInfo("com.local.listentomusic", 0).uid;
            AudioPlaybackCaptureConfiguration capture = new AudioPlaybackCaptureConfiguration.Builder(projection)
                .addMatchingUid(target).addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME).addMatchingUsage(AudioAttributes.USAGE_UNKNOWN).build();
            int rate = 44100;
            AudioFormat format = new AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO).build();
            AudioRecord audio = new AudioRecord.Builder().setAudioFormat(format).setAudioPlaybackCaptureConfig(capture)
                .setBufferSizeInBytes(Math.max(16384, AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) * 4)).build();
            if (audio.getState() != AudioRecord.STATE_INITIALIZED) { audio.release(); throw new IllegalStateException("Uninitialized capture"); }
            recorder = audio;
            String requested = intent.getStringExtra("name");
            String name = requested != null && requested.matches("[a-z0-9_-]{1,40}\\.wav") ? requested : "stack-output.wav";
            File destination = new File(getExternalFilesDir(null), name);
            if (destination.exists()) throw new IllegalStateException("Never overwrite a captured test");
            new Thread(() -> {
                try (RandomAccessFile file = new RandomAccessFile(destination, "rw")) {
                    file.write(new byte[44]);
                    short[] samples = new short[4096];
                    ByteBuffer bytes = ByteBuffer.allocate(samples.length * 2).order(ByteOrder.LITTLE_ENDIAN);
                    int count = 0;
                    long end = SystemClock.elapsedRealtime() + 70000;
                    audio.startRecording();
                    Log.i("GreaterArtOutputCapture", "START uid=" + target + " file=" + destination);
                    if (callback != null) callback.send(0, new Bundle());
                    while (active.get() && count < rate * 70 && SystemClock.elapsedRealtime() < end) {
                        int read = audio.read(samples, 0, Math.min(samples.length, rate * 70 - count));
                        if (read <= 0) throw new IllegalStateException("Audio capture read=" + read);
                        bytes.clear(); for (int i = 0; i < read; i++) bytes.putShort(samples[i]);
                        file.write(bytes.array(), 0, read * 2); count += read;
                    }
                    int size = count * 2;
                    ByteBuffer header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
                    header.put("RIFF".getBytes()).putInt(size + 36).put("WAVEfmt ".getBytes())
                        .putInt(16).putShort((short)1).putShort((short)1).putInt(rate).putInt(rate * 2)
                        .putShort((short)2).putShort((short)16).put("data".getBytes()).putInt(size);
                    file.seek(0); file.write(header.array());
                    Log.i("GreaterArtOutputCapture", "DONE samples=" + count + " file=" + destination);
                } catch (Exception failure) {
                    Log.e("GreaterArtOutputCapture", "Capture failed", failure);
                    if (callback != null) callback.send(-1, new Bundle());
                } finally {
                    stopRecorder(); audio.release(); recorder = null;
                    if (projection != null) projection.stop(); stopSelf();
                }
            }, "stack-output-test").start();
        } catch (Exception failure) {
            Log.e("GreaterArtOutputCapture", "Could not start", failure);
            if (callback != null) callback.send(-1, new Bundle());
            active.set(false); stopRecorder();
            if (recorder != null) { recorder.release(); recorder = null; }
            if (projection != null) projection.stop(); stopSelf();
        }
        return START_NOT_STICKY;
    }

    private void stopRecorder() { try { if (recorder != null) recorder.stop(); } catch (IllegalStateException ignored) { } }
    @Override public void onDestroy() {
        active.set(false); stopRecorder(); if (projection != null) projection.stop(); super.onDestroy();
    }
}
