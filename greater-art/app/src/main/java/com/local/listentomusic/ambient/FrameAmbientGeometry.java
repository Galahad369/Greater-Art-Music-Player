package com.local.listentomusic.ambient;

/** One proportional scale, with clipping for Crop. */
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

    /** Compose pan is top-down; GL stage coordinates are bottom-up. Scale uniformly. */
    public static float[] transform(float[] rect, float zoom, float panX, float panY) {
        if (rect == null || rect.length != 4 || !Float.isFinite(zoom) || zoom < 1f
                || !Float.isFinite(panX) || !Float.isFinite(panY)
                || !Float.isFinite(rect[2]) || !Float.isFinite(rect[3])
                || rect[2] <= 0f || rect[3] <= 0f)
            throw new IllegalArgumentException("Invalid video transform");
        float width = rect[2] * zoom;
        float height = rect[3] * zoom;
        if (!Float.isFinite(width) || !Float.isFinite(height))
            throw new IllegalArgumentException("Video transform exceeds finite bounds");
        return new float[] {
            rect[0] - (width - rect[2]) / 2f + panX,
            rect[1] - (height - rect[3]) / 2f - panY,
            width, height
        };
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
