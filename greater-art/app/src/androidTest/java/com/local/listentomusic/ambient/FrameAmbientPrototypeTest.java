package com.local.listentomusic.ambient;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import androidx.media3.common.C;
import androidx.media3.common.util.UnstableApi;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Real Media3 -> external texture -> video + light pixels; timestamps alone cannot pass. */
@UnstableApi
@RunWith(AndroidJUnit4.class)
public class FrameAmbientPrototypeTest {
    @Test public void rapidSceneCutsKeepVideoAndGlowOnTheSameFrame() throws Exception {
        runProbe(true, false, activity -> {
            await(activity, () -> activity.probeView().snapshot().size() >= 80);
            List<FrameAmbientView.Sample> samples = activity.probeView().snapshot();
            int mismatches = inspect(samples);
            assertEquals("FRAME_GLOW_DESYNC: actual video and glow colors differ", 0, mismatches);
            save(activity, "same-frame", samples);
            android.util.Log.i("GA-FrameAmbient", "FRAME_AMBIENT_SYNC_PASS samples=" + samples.size()
                    + " mismatches=" + mismatches + " renderer=" + activity.probeView().rendererName());
        });
    }

    @Test public void retainedStalePaletteIsCaughtEvenWhenTimestampsMatch() throws Exception {
        runProbe(true, true, activity -> {
            await(activity, () -> activity.probeView().snapshot().size() >= 80);
            List<FrameAmbientView.Sample> samples = activity.probeView().snapshot();
            int mismatches = inspect(samples);
            assertTrue("Negative control must expose stale-color pixels, not just matching frame IDs", mismatches >= 12);
            save(activity, "stale-palette-negative-control", samples);
            android.util.Log.i("GA-FrameAmbient", "FRAME_AMBIENT_NEGATIVE_CONTROL_PASS mismatches=" + mismatches);
        });
    }

    @Test public void fitCropPauseSeekAndRotationKeepPlayerAndNativeGeometry() throws Exception {
        runProbe(true, false, activity -> {
            Object player = onMain(activity::probePlayer);
            await(activity, () -> activity.probeView().snapshot().size() >= 30);
            onMain(() -> { activity.probePlayer().pause(); return null; });
            // pause() is asynchronous and does not revoke a frame already queued
            // to SurfaceTexture. Drain it within the same bounded readiness gate.
            int[] drainingFrames = {activity.probeView().renderedFrames()};
            long[] quietSince = {System.nanoTime()};
            await(activity, () -> {
                int frames = activity.probeView().renderedFrames();
                if (frames != drainingFrames[0]) {
                    drainingFrames[0] = frames;
                    quietSince[0] = System.nanoTime();
                }
                return System.nanoTime() - quietSince[0] >= TimeUnit.MILLISECONDS.toNanos(500);
            });
            int pausedFrames = activity.probeView().renderedFrames();
            long pausedPosition = onMain(() -> activity.probePlayer().getCurrentPosition());
            Thread.sleep(300);
            assertEquals("Paused source must not keep producing diagnostic frames", pausedFrames,
                    activity.probeView().renderedFrames());
            assertEquals("Paused media clock must also remain stopped", pausedPosition,
                    (long) onMain(() -> activity.probePlayer().getCurrentPosition()));
            verifySquare(activity, "portrait-fit");
            onMain(() -> { activity.probeView().setCrop(true); return null; });
            await(activity, () -> activity.probeView().videoRectangle()[2] > activity.probeView().stageRectangle()[2]);
            verifySquare(activity, "portrait-crop");
            assertEquals("Fit/Crop is a redraw, not a decoder restart", pausedFrames, activity.probeView().renderedFrames());
            onMain(() -> { activity.probePlayer().seekTo(4000); return null; });
            await(activity, () -> activity.probeView().snapshot().stream().anyMatch(sample ->
                    sample.presentationTimeUs >= 3_900_000 && sample.presentationTimeUs <= 4_100_000));
            onMain(() -> {
                activity.probeView().setCrop(false);
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                return null;
            });
            await(activity, () -> activity.probeView().getWidth() > activity.probeView().getHeight());
            await(activity, () -> {
                float[] rectangle = activity.probeView().videoRectangle();
                return rectangle != null && Math.abs(rectangle[2] / rectangle[3] - 16f / 9) < .001f;
            });
            verifySquare(activity, "landscape-fit");
            onMain(() -> { activity.probePlayer().play(); return null; });
            await(activity, () -> activity.probeView().snapshot().stream().filter(sample -> sample.width > sample.height).count() >= 20);
            onMain(() -> {
                assertSame("Orientation must keep the one test player", player, activity.probePlayer());
                assertEquals(1280, activity.probePlayer().getVideoSize().width);
                assertEquals(720, activity.probePlayer().getVideoSize().height);
                return null;
            });
            assertEquals(0, inspect(activity.probeView().snapshot()));
            save(activity, "transport-geometry", activity.probeView().snapshot());
            android.util.Log.i("GA-FrameAmbient", "FRAME_AMBIENT_TRANSPORT_GEOMETRY_PASS primary1280x720 player-retained");
        });
    }

    @Test public void normalPrototypeDoesNotReadPixelsEveryFrame() throws Exception {
        runProbe(false, false, activity -> {
            await(activity, () -> activity.probeView().renderedFrames() >= 30);
            assertTrue("CPU readback is instrumentation-only", activity.probeView().snapshot().isEmpty());
            assertTrue(activity.formatAccepted());
            android.util.Log.i("GA-FrameAmbient", "FRAME_AMBIENT_NO_READBACK_PASS rendered=" + activity.probeView().renderedFrames());
        });
    }

    private interface Probe { void run(AmbientFrameProbeActivity activity) throws Exception; }
    private interface Condition { boolean ready() throws Exception; }

    private void runProbe(boolean capture, boolean stale, Probe probe) throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File root = new File(context.getCacheDir(), "ambient-frame-probe");
        assertTrue(root.isDirectory() || root.mkdirs());
        File fixture = new File(root, UUID.randomUUID() + ".mp4");
        ActivityScenario<AmbientFrameProbeActivity> scenario = null;
        AmbientFrameProbeActivity activity = null;
        try {
            try (InputStream input = InstrumentationRegistry.getInstrumentation().getContext().getAssets()
                    .open("ambient-frame-cuts-sdr.mp4"); FileOutputStream output = new FileOutputStream(fixture)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            }
            Intent intent = new Intent(context, AmbientFrameProbeActivity.class)
                    .setData(Uri.fromFile(fixture)).putExtra(AmbientFrameProbeActivity.CAPTURE, capture)
                    .putExtra(AmbientFrameProbeActivity.STALE_CONTROL, stale);
            scenario = ActivityScenario.launch(intent);
            activity = onMain(() -> ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                    .stream().filter(item -> item instanceof AmbientFrameProbeActivity)
                    .map(item -> (AmbientFrameProbeActivity) item).findFirst()
                    .orElseThrow(() -> new AssertionError("Owned probe Activity was not resumed")));
            AmbientFrameProbeActivity owner = activity;
            onMain(() -> { owner.probePlayer().play(); return null; });
            probe.run(activity);
        } finally {
            if (activity != null) {
                AmbientFrameProbeActivity owner = activity;
                onMain(() -> { owner.finish(); return null; });
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                while (!onMain(owner::isDestroyed) && System.nanoTime() < deadline) Thread.sleep(20);
                assertTrue("Owned native Activity must be destroyed", onMain(owner::isDestroyed));
            }
            if (scenario != null) scenario.close();
            assertTrue("Remove only the owned fixture", !fixture.exists() || fixture.delete());
        }
    }

    private static <T> T onMain(Callable<T> callable) throws Exception {
        FutureTask<T> future = new FutureTask<>(callable);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(future);
        return future.get(5, TimeUnit.SECONDS);
    }

    private static void await(AmbientFrameProbeActivity activity, Condition condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (System.nanoTime() < deadline) {
            assertTrue("Prototype failure: " + activity.failure(), activity.failure() == null);
            if (condition.ready()) return;
            Thread.sleep(20);
        }
        throw new AssertionError("Prototype condition timed out; renderer=" + activity.probeView().rendererName()
                + " frames=" + activity.probeView().renderedFrames() + " accepted=" + activity.formatAccepted());
    }

    private static int inspect(List<FrameAmbientView.Sample> samples) {
        assertTrue("Enough actual sampled frames", samples.size() >= 30);
        Set<Long> timestamps = new HashSet<>();
        Set<Integer> colors = new HashSet<>();
        int transitions = 0;
        int previous = -1;
        int mismatch = 0;
        int metadata = 0;
        for (FrameAmbientView.Sample sample : samples) {
            assertTrue("No repeated frame observations", timestamps.add(sample.textureTimestampNs));
            int foreground = FrameAmbientGeometry.dominant(sample.videoRgb);
            int glow = FrameAmbientGeometry.dominant(sample.glowRgb);
            assertTrue("Black/neutral pixels cannot prove alignment", foreground >= 0 && glow >= 0);
            colors.add(foreground);
            if (previous >= 0 && foreground != previous) transitions++;
            previous = foreground;
            if (foreground != glow) mismatch++;
            if (sample.presentationTimeUs != C.TIME_UNSET) metadata++;
        }
        assertEquals("All generated source colors must actually render", 3, colors.size());
        assertTrue("Observe real cuts, not a single static color", transitions >= 9);
        assertTrue("Correlate actual Media3 presentation metadata too", metadata >= 20);
        return mismatch;
    }

    private static void save(AmbientFrameProbeActivity activity, String label, List<FrameAmbientView.Sample> samples)
            throws Exception {
        File root = new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null),
                "ambient-frame-probe");
        assertTrue(root.isDirectory() || root.mkdirs());
        File file = new File(root, label + "-" + UUID.randomUUID() + ".csv");
        assertTrue("Never overwrite probe evidence", file.createNewFile());
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write("texture_ns,presentation_us,observed_ns,video_rgb,glow_rgb,width,height,crop\n"
                    .getBytes(StandardCharsets.UTF_8));
            for (FrameAmbientView.Sample sample : samples) {
                output.write((sample.textureTimestampNs + "," + sample.presentationTimeUs + "," + sample.observedNs
                        + "," + sample.videoRgb + "," + sample.glowRgb + "," + sample.width + "," + sample.height
                        + "," + sample.crop + "\n").getBytes(StandardCharsets.UTF_8));
            }
        }
        android.util.Log.i("GA-FrameAmbient", "evidence=" + file.getName() + " renderer=" + activity.probeView().rendererName());
    }

    private static void verifySquare(AmbientFrameProbeActivity activity, String label) throws Exception {
        // The fixture contains one 80x80 white outline. Check its real displayed shape.
        await(activity, () -> onMain(() -> activity.probeView().hasDrawnViewport(
                activity.probeView().getWidth(), activity.probeView().getHeight())));
        // Reuse FullscreenRotationTest's paused-compositor stability pattern.
        // Relayout precedes Android's rotation animation; do not measure its
        // temporary screen transform as if it were the settled video rectangle.
        Bitmap[] settled = {null};
        int[] stableSamples = {0};
        long[] sampledAt = {0};
        try {
            await(activity, () -> {
                long now = System.nanoTime();
                if (now - sampledAt[0] < TimeUnit.MILLISECONDS.toNanos(100)) return false;
                sampledAt[0] = now;
                Bitmap next = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
                if (next == null) { stableSamples[0] = 0; return false; }
                stableSamples[0] = settled[0] != null && settled[0].sameAs(next) ? stableSamples[0] + 1 : 0;
                if (settled[0] != null) settled[0].recycle();
                settled[0] = next;
                return stableSamples[0] >= 3;
            });
        } catch (Exception | AssertionError error) {
            if (settled[0] != null) settled[0].recycle();
            throw error;
        }
        int[] location = onMain(() -> {
            int[] point = new int[4];
            activity.probeView().getLocationOnScreen(point);
            point[2] = activity.probeView().getWidth(); point[3] = activity.probeView().getHeight();
            return point;
        });
        float[] stage = activity.probeView().stageRectangle();
        assertNotNull(stage);
        Bitmap bitmap = settled[0];
        assertNotNull("Native screenshot required", bitmap);
        try {
            // Preserve failed frames too; an assertion must not discard its evidence.
            File root = new File(activity.getExternalFilesDir(null), "ambient-frame-probe");
            assertTrue(root.isDirectory() || root.mkdirs());
            File capture = new File(root, label + "-" + UUID.randomUUID() + ".png");
            assertTrue(capture.createNewFile());
            try (FileOutputStream output = new FileOutputStream(capture)) {
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output));
            }
            android.util.Log.i("GA-FrameAmbient", "geometry=" + label + " capture=" + capture.getName()
                    + " screenshot=" + bitmap.getWidth() + "x" + bitmap.getHeight()
                    + " view=" + java.util.Arrays.toString(location)
                    + " stage=" + java.util.Arrays.toString(stage));
            int left = Math.max(0, location[0]);
            int right = Math.min(bitmap.getWidth(), left + location[2]);
            int top = Math.max(0, Math.round(location[1] + location[3] - stage[1] - stage[3]));
            int bottom = Math.min(bitmap.getHeight(), Math.round(location[1] + location[3] - stage[1]));
            int minX = right, maxX = -1, minY = bottom, maxY = -1;
            for (int y = top; y < bottom; y++) for (int x = left; x < right; x++) {
                int rgb = bitmap.getPixel(x, y);
                int r = (rgb >>> 16) & 255, g = (rgb >>> 8) & 255, b = rgb & 255;
                if (Math.min(r, Math.min(g, b)) >= 230) {
                    minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                }
            }
            assertTrue("Native marker must be visible: " + label, maxX > minX && maxY > minY);
            assertEquals("Source square must not become a stretched rectangle: " + label,
                    maxX - minX, maxY - minY, 4);
        } finally { bitmap.recycle(); }
    }
}
