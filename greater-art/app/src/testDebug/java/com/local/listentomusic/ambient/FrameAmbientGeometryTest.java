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
    @Test public void colorProbeCannotTreatBlackOrNeutralAsMatchingScenes() {
        assertEquals(-1, FrameAmbientGeometry.dominant(0));
        assertEquals(-1, FrameAmbientGeometry.dominant(0x777777));
        assertEquals(0, FrameAmbientGeometry.dominant(0xf00302));
        assertEquals(1, FrameAmbientGeometry.dominant(0x027003));
        assertEquals(2, FrameAmbientGeometry.dominant(0x03027f));
    }
}
