package com.local.listentomusic.ambient;

/** Debug-prototype layout math. One proportional scale, with clipping for Crop. */
public final class FrameAmbientGeometry {
    private FrameAmbientGeometry() {}

    public static float[] place(float x, float y, float width, float height,
            float sourceAspect, boolean crop) {
        if (!Float.isFinite(sourceAspect) || !Float.isFinite(x) || !Float.isFinite(y)
                || !Float.isFinite(width) || !Float.isFinite(height)
                || sourceAspect <= 0 || width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Invalid frame geometry");
        }
        boolean widthLimited = (sourceAspect >= width / height) != crop;
        float drawnWidth = widthLimited ? width : height * sourceAspect;
        float drawnHeight = widthLimited ? width / sourceAspect : height;
        return new float[] {x + (width - drawnWidth) / 2, y + (height - drawnHeight) / 2,
                drawnWidth, drawnHeight};
    }

    public static int dominant(int rgb) {
        int r = (rgb >>> 16) & 255;
        int g = (rgb >>> 8) & 255;
        int b = rgb & 255;
        int max = Math.max(r, Math.max(g, b));
        int second = r + g + b - max - Math.min(r, Math.min(g, b));
        if (max < 28 || max - second < 20) return -1;
        return max == r ? 0 : max == g ? 1 : 2;
    }
}
