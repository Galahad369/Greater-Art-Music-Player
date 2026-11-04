package com.local.listentomusic.ambient;

import androidx.media3.common.C;
import androidx.media3.common.ColorInfo;
import androidx.media3.common.Format;
import androidx.media3.common.util.UnstableApi;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@UnstableApi
public class FrameAmbientFormatPolicyTest {
    private Format format(ColorInfo color) {
        return new Format.Builder().setWidth(3840).setHeight(2160).setColorInfo(color).build();
    }
    private ColorInfo.Builder sdr() {
        return new ColorInfo.Builder().setColorSpace(C.COLOR_SPACE_BT709)
                .setColorTransfer(C.COLOR_TRANSFER_SDR).setLumaBitdepth(8).setChromaBitdepth(8);
    }
    @Test public void nativeFourKIsAllowedWithoutAResolutionCap() {
        assertTrue(FrameAmbientFormatPolicy.accepts(format(sdr().build())));
    }
    @Test public void hdrHighPrecisionAndUnknownInputsAreRefused() {
        assertFalse(FrameAmbientFormatPolicy.accepts(format(sdr().setColorTransfer(C.COLOR_TRANSFER_HLG).build())));
        assertFalse(FrameAmbientFormatPolicy.accepts(format(sdr().setColorTransfer(C.COLOR_TRANSFER_ST2084).build())));
        assertFalse(FrameAmbientFormatPolicy.accepts(format(sdr().setLumaBitdepth(10).setChromaBitdepth(10).build())));
        assertFalse(FrameAmbientFormatPolicy.accepts(format(sdr().setLumaBitdepth(Format.NO_VALUE).build())));
        assertFalse(FrameAmbientFormatPolicy.accepts(format(sdr().setColorSpace(C.COLOR_SPACE_BT2020).build())));
        assertFalse(FrameAmbientFormatPolicy.accepts(format(null)));
    }
}
