package com.local.listentomusic.ambient;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class FrameAmbientGeometryTest {
    @Test public void fitAndCropPreserveSourceAcrossPhoneSizes() {
        for (float[] size : new float[][] {{1080, 2340}, {2340, 1080}, {320, 640}, {640, 320}}) {
            for (float ratio : new float[] {16f / 9, 9f / 16, 1f, 4f / 3}) {
                for (boolean crop : new boolean[] {false, true}) {
                    float[] rectangle = FrameAmbientGeometry.place(7, 11, size[0], size[1], ratio, crop);
                    assertEquals(ratio, rectangle[2] / rectangle[3], .00001f);
                    assertEquals(7 + size[0] / 2, rectangle[0] + rectangle[2] / 2, .001f);
                    assertEquals(11 + size[1] / 2, rectangle[1] + rectangle[3] / 2, .001f);
                    if (crop) assertTrue(rectangle[2] >= size[0] - .001f && rectangle[3] >= size[1] - .001f);
                    else assertTrue(rectangle[2] <= size[0] + .001f && rectangle[3] <= size[1] + .001f);
                }
            }
        }
    }
    @Test public void invalidMetadataIsRefusedNotStretched() {
        for (float ratio : new float[] {0, -1, Float.NaN, Float.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> FrameAmbientGeometry.place(0, 0, 100, 100, ratio, false));
        }
        assertThrows(IllegalArgumentException.class, () -> FrameAmbientGeometry.place(0, 0, 0, 100, 1, true));
    }
    @Test public void pinchPanMaintainsAspectAndMatchesNativeComposeCoordinates() {
        float[] baseline = FrameAmbientGeometry.place(0, 100, 720, 420, 16f / 9f, false);
        float[] same = FrameAmbientGeometry.transform(baseline, 1f, 0f, 0f);
        for (int i = 0; i < 4; i++) assertEquals(baseline[i], same[i], .00001f);
        float[] zoomed = FrameAmbientGeometry.transform(baseline, 2f, 20f, 30f);
        assertEquals(baseline[2] * 2f, zoomed[2], .001f);
        assertEquals(baseline[3] * 2f, zoomed[3], .001f);
        assertEquals(baseline[2] / baseline[3], zoomed[2] / zoomed[3], .00001f);
        assertEquals(baseline[0] - baseline[2] / 2f + 20f, zoomed[0], .001f);
        assertEquals(baseline[1] - baseline[3] / 2f - 30f, zoomed[1], .001f);
        assertThrows(IllegalArgumentException.class,
            () -> FrameAmbientGeometry.transform(baseline, Float.NaN, 0f, 0f));
        assertThrows(IllegalArgumentException.class,
            () -> FrameAmbientGeometry.transform(baseline, 0f, 0f, 0f));
    }

    @Test public void colorProbeCannotTreatBlackOrNeutralAsMatchingScenes() {
        assertEquals(-1, FrameAmbientGeometry.dominant(0));
        assertEquals(-1, FrameAmbientGeometry.dominant(0x777777));
        assertEquals(0, FrameAmbientGeometry.dominant(0xf00302));
        assertEquals(1, FrameAmbientGeometry.dominant(0x027003));
        assertEquals(2, FrameAmbientGeometry.dominant(0x03027f));
    }
}
