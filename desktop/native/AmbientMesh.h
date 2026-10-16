#pragma once
#include <windows.h>
#include <algorithm>
#include <array>
#include <cmath>
#include <cstdint>

namespace ga
{
// A native vector color mesh, not a scaled video/image. Windows interpolates 16-bit
// vertex colors directly at destination pixels and respects the DC's dirty region.
inline void ambientMesh(HDC dc, const RECT &clip, int width, int height, COLORREF base,
                        const COLORREF *palette, double brightness, double saturation, double phase)
{
    constexpr int X = 24, Y = 16;
    std::array<TRIVERTEX, (X + 1) * (Y + 1)> vertices{};
    static const auto triangles = [] {
        std::array<GRADIENT_TRIANGLE, X * Y * 2> result{};
        int i = 0;
        for (DWORD y = 0; y < Y; ++y)
            for (DWORD x = 0; x < X; ++x)
            {
                DWORD a = y * (X + 1) + x, b = a + 1, c = a + X + 1, d = c + 1;
                result[i++] = {a, b, c};
                result[i++] = {b, d, c};
            }
        return result;
    }();
    double radius = std::max(width, height) * .65;
    double intensity = (10 + 17 * brightness + 12 * saturation) / 255;
    auto channel = [](COLORREF c, int index) {
        return index == 0 ? GetRValue(c) : index == 1 ? GetGValue(c) : GetBValue(c);
    };
    for (int y = 0; y <= Y; ++y)
        for (int x = 0; x <= X; ++x)
        {
            auto &v = vertices[y * (X + 1) + x];
            v.x = MulDiv(x, width, X);
            v.y = MulDiv(y, height, Y);
            double weight[2]{};
            for (int i = 0; i < 2; ++i)
            {
                double cx = width * (.25 + .5 * i + .025 * std::sin(phase + i));
                double cy = height * (.28 + .35 * i + .025 * std::cos(phase + i));
                weight[i] = intensity * std::max(0.0, 1 - std::hypot(v.x - cx, v.y - cy) / radius);
            }
            COLOR16 *channels[]{&v.Red, &v.Green, &v.Blue};
            for (int c = 0; c < 3; ++c)
            {
                double tint =
                    channel(palette[0], c) * (1 - double(y) / Y) + channel(palette[1], c) * double(y) / Y;
                double value = (channel(base, c) * 4 + tint) / 5;
                for (int i = 0; i < 2; ++i)
                    value += (channel(palette[i], c) - value) * weight[i];
                *channels[c] = COLOR16(std::clamp(value * 256, 0.0, 65535.0));
            }
        }
    int saved = SaveDC(dc);
    IntersectClipRect(dc, clip.left, clip.top, clip.right, clip.bottom);
    GradientFill(dc, vertices.data(), DWORD(vertices.size()),
                 const_cast<GRADIENT_TRIANGLE *>(triangles.data()), DWORD(triangles.size()),
                 GRADIENT_FILL_TRIANGLE);
    RestoreDC(dc, saved);
}
class AmbientLayer
{
    HDC dc = nullptr;
    HBITMAP bitmap = nullptr;
    HGDIOBJ old = nullptr;
    int width = 0, height = 0;
    COLORREF base = 0, colors[2]{};
    double brightness = -1, saturation = -1;
    long long position = -1;
    void clear()
    {
        if (dc && old)
            SelectObject(dc, old);
        if (bitmap)
            DeleteObject(bitmap);
        if (dc)
            DeleteDC(dc);
        dc = nullptr;
        bitmap = nullptr;
        old = nullptr;
    }

  public:
    ~AmbientLayer()
    {
        clear();
    }
    void paint(HDC target, const RECT &clip, int w, int h, COLORREF color, const COLORREF *palette,
               double light, double chroma, double phase)
    {
        // Full destination resolution. Only subpixel shifts of the very soft
        // lighting centers share a cache entry; there is no frame-rate limit.
        double steps = std::max(w, h) * .025 * 8;
        auto location = static_cast<long long>(std::floor(phase * steps));
        if (w <= 0 || h <= 0 || std::uint64_t(w) * h > 8388608)
        {
            ambientMesh(target, clip, w, h, color, palette, light, chroma, phase);
            return;
        }
        bool resize = w != width || h != height || !dc;
        if (resize)
        {
            clear();
            dc = CreateCompatibleDC(target);
            bitmap = CreateCompatibleBitmap(target, w, h);
            if (!dc || !bitmap)
            {
                clear();
                ambientMesh(target, clip, w, h, color, palette, light, chroma, phase);
                return;
            }
            old = SelectObject(dc, bitmap);
            width = w;
            height = h;
        }
        if (resize || base != color || colors[0] != palette[0] || colors[1] != palette[1] ||
            brightness != light || saturation != chroma || position != location)
        {
            RECT whole{0, 0, w, h};
            ambientMesh(dc, whole, w, h, color, palette, light, chroma, location / steps);
            base = color;
            colors[0] = palette[0];
            colors[1] = palette[1];
            brightness = light;
            saturation = chroma;
            position = location;
        }
        BitBlt(target, clip.left, clip.top, clip.right - clip.left, clip.bottom - clip.top, dc, clip.left,
               clip.top, SRCCOPY);
    }
};
} // namespace ga
