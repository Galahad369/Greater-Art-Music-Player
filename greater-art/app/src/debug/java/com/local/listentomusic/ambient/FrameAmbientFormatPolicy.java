package com.local.listentomusic.ambient;

import androidx.media3.common.C;
import androidx.media3.common.ColorInfo;
import androidx.media3.common.Format;
import androidx.media3.common.util.UnstableApi;

/** Safe refusal is not HDR/high-bit-depth support. No implicit tone mapping. */
@UnstableApi
public final class FrameAmbientFormatPolicy {
    private FrameAmbientFormatPolicy() {}

    public static boolean accepts(Format format) {
        ColorInfo color = format.colorInfo;
        return format.width > 0 && format.height > 0 && color != null
                && color.colorTransfer == C.COLOR_TRANSFER_SDR
                && color.lumaBitdepth == 8 && color.chromaBitdepth == 8
                && color.hdrStaticInfo == null && color.colorSpace != C.COLOR_SPACE_BT2020;
    }
}
