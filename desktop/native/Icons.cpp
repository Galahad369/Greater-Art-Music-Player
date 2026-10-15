#define NOMINMAX
#include "Icons.h"
#include <gdiplus.h>
#include <array>
#include <cmath>
#include <fstream>
#include <vector>

namespace ga
{
namespace
{
Gdiplus::Color tint(COLORREF c)
{
    return {255, GetRValue(c), GetGValue(c), GetBValue(c)};
}
void render(Gdiplus::Graphics &g, Icon icon, float cx, float cy, float size, COLORREF color)
{
    auto state = g.Save();
    g.SetSmoothingMode(Gdiplus::SmoothingModeAntiAlias);
    g.SetPixelOffsetMode(Gdiplus::PixelOffsetModeHighQuality);
    Gdiplus::Matrix matrix(size / 24, 0, 0, size / 24, cx - size / 2, cy - size / 2);
    g.SetTransform(&matrix);
    Gdiplus::Pen pen(tint(color), 1.8f);
    pen.SetStartCap(Gdiplus::LineCapRound);
    pen.SetEndCap(Gdiplus::LineCapRound);
    pen.SetLineJoin(Gdiplus::LineJoinRound);
    Gdiplus::SolidBrush brush(tint(color));
    auto line = [&](float a, float b, float c, float d) { g.DrawLine(&pen, a, b, c, d); };
    auto dot = [&](float x, float y, float radius) {
        g.FillEllipse(&brush, x - radius, y - radius, 2 * radius, 2 * radius);
    };
    auto polygon = [&](std::initializer_list<Gdiplus::PointF> points, bool fill = false) {
        std::vector<Gdiplus::PointF> p(points);
        if (fill)
            g.FillPolygon(&brush, p.data(), int(p.size()));
        else
            g.DrawPolygon(&pen, p.data(), int(p.size()));
    };
    auto corners = [&] {
        line(4, 9, 4, 4);
        line(4, 4, 9, 4);
        line(15, 4, 20, 4);
        line(20, 4, 20, 9);
        line(20, 15, 20, 20);
        line(20, 20, 15, 20);
        line(9, 20, 4, 20);
        line(4, 20, 4, 15);
    };
    switch (icon)
    {
    case Icon::Play:
        polygon({{8, 4.5f}, {20, 12}, {8, 19.5f}}, true);
        break;
    case Icon::Pause:
        pen.SetWidth(4);
        line(8, 6, 8, 18);
        line(16, 6, 16, 18);
        break;
    case Icon::Previous:
        polygon({{18, 5}, {8, 12}, {18, 19}}, true);
        line(5, 5, 5, 19);
        break;
    case Icon::Next:
        polygon({{6, 5}, {16, 12}, {6, 19}}, true);
        line(19, 5, 19, 19);
        break;
    case Icon::Repeat:
    case Icon::RepeatOne:
        line(4, 10, 4, 8);
        line(4, 8, 20, 8);
        line(17, 5, 20, 8);
        line(20, 8, 17, 11);
        line(20, 14, 20, 16);
        line(20, 16, 4, 16);
        line(7, 13, 4, 16);
        line(4, 16, 7, 19);
        if (icon == Icon::RepeatOne)
        {
            line(10.5f, 11, 12, 10);
            line(12, 10, 12, 14);
        }
        break;
    case Icon::Library:
        line(4, 5, 4, 20);
        line(8, 5, 8, 20);
        line(12, 5, 12, 20);
        polygon({{16, 5}, {19, 4}, {22, 19}, {19, 20}});
        break;
    case Icon::Stack:
        polygon({{3, 7}, {12, 3}, {21, 7}, {12, 11}});
        line(3, 12, 12, 16);
        line(12, 16, 21, 12);
        line(3, 17, 12, 21);
        line(12, 21, 21, 17);
        break;
    case Icon::Nodes:
        line(6, 6, 18, 8);
        line(6, 6, 10, 19);
        line(18, 8, 10, 19);
        dot(6, 6, 3);
        dot(18, 8, 3);
        dot(10, 19, 3);
        break;
    case Icon::Settings: {
        std::array<Gdiplus::PointF, 32> points{};
        for (int i = 0; i < 32; ++i)
        {
            float a = float(i * 6.283185307 / 32), r = (i % 4 == 1 || i % 4 == 2) ? 9.f : 7.2f;
            points[i] = {12 + r * std::cos(a), 12 + r * std::sin(a)};
        }
        g.DrawPolygon(&pen, points.data(), int(points.size()));
        g.DrawEllipse(&pen, 8.7f, 8.7f, 6.6f, 6.6f);
        break;
    }
    case Icon::Refresh:
        g.DrawArc(&pen, 4.f, 4.f, 16.f, 16.f, 20.f, 290.f);
        line(17, 4, 20, 7);
        line(20, 7, 20, 2.5f);
        break;
    case Icon::History:
        g.DrawArc(&pen, 4.f, 4.f, 16.f, 16.f, 215.f, 310.f);
        line(3, 3, 3, 8);
        line(3, 8, 8, 8);
        line(12, 7, 12, 12);
        line(12, 12, 16, 14);
        break;
    case Icon::More:
        dot(5, 12, 1.6f);
        dot(12, 12, 1.6f);
        dot(19, 12, 1.6f);
        break;
    case Icon::Plus:
        line(5, 12, 19, 12);
        line(12, 5, 12, 19);
        break;
    case Icon::Close:
        line(5, 5, 19, 19);
        line(19, 5, 5, 19);
        break;
    case Icon::Search:
        g.DrawEllipse(&pen, 3.5f, 3.5f, 12.f, 12.f);
        line(14, 14, 20, 20);
        break;
    case Icon::Sort:
        line(4, 6, 20, 6);
        line(4, 12, 15, 12);
        line(4, 18, 10, 18);
        break;
    case Icon::Folder:
        polygon({{3, 6}, {10, 6}, {12, 9}, {21, 9}, {21, 19}, {3, 19}});
        break;
    case Icon::Save:
        polygon({{4, 3}, {17, 3}, {21, 7}, {21, 21}, {4, 21}});
        polygon({{8, 3}, {8, 9}, {16, 9}, {16, 3}});
        polygon({{8, 21}, {8, 14}, {17, 14}, {17, 21}});
        break;
    case Icon::Expand:
        corners();
        break;
    case Icon::Collapse:
        polygon({{3, 4}, {21, 4}, {21, 20}, {3, 20}});
        polygon({{12, 12}, {18, 12}, {18, 17}, {12, 17}}, true);
        break;
    case Icon::Home:
        polygon({{3, 11},
                 {12, 3},
                 {21, 11},
                 {19, 11},
                 {19, 21},
                 {14, 21},
                 {14, 15},
                 {10, 15},
                 {10, 21},
                 {5, 21},
                 {5, 11}});
        break;
    case Icon::Lock:
    case Icon::Unlock:
        g.DrawRectangle(&pen, 5.f, 10.f, 14.f, 11.f);
        if (icon == Icon::Lock)
        {
            g.DrawArc(&pen, 8.f, 3.f, 8.f, 8.f, 180.f, 180.f);
            line(8, 7, 8, 10);
            line(16, 7, 16, 10);
        }
        else
        {
            g.DrawArc(&pen, 9.f, 3.f, 8.f, 8.f, 180.f, 180.f);
            line(9, 7, 9, 10);
        }
        dot(12, 15, 1);
        line(12, 15, 12, 17);
        break;
    case Icon::Share:
        line(6, 12, 18, 6);
        line(6, 12, 18, 18);
        dot(6, 12, 2.8f);
        dot(18, 5, 2.8f);
        dot(18, 19, 2.8f);
        break;
    case Icon::Video:
        polygon({{3, 6}, {15, 6}, {15, 18}, {3, 18}});
        polygon({{15, 10}, {21, 7}, {21, 17}, {15, 14}});
        break;
    case Icon::Music:
        line(9, 17, 9, 5);
        line(9, 5, 20, 3);
        line(20, 3, 20, 15);
        line(9, 8, 20, 6);
        g.FillEllipse(&brush, 3.f, 15.f, 6.f, 5.f);
        g.FillEllipse(&brush, 14.f, 13.f, 6.f, 5.f);
        break;
    case Icon::Magic:
        polygon({{4, 18}, {16, 6}, {19, 9}, {7, 21}});
        line(12, 10, 15, 13);
        line(6, 2, 6, 8);
        line(3, 5, 9, 5);
        line(20, 15, 20, 21);
        line(17, 18, 23, 18);
        break;
    case Icon::Fit:
        corners();
        line(8, 12, 16, 12);
        line(12, 8, 12, 16);
        break;
    case Icon::Heart:
    case Icon::HeartFilled: {
        Gdiplus::GraphicsPath heart;
        heart.AddBezier(12.f, 7.f, 7.f, -1.f, -1.f, 8.f, 5.f, 14.f);
        heart.AddBezier(5.f, 14.f, 7.f, 16.f, 10.f, 19.f, 12.f, 21.f);
        heart.AddBezier(12.f, 21.f, 14.f, 19.f, 17.f, 16.f, 19.f, 14.f);
        heart.AddBezier(19.f, 14.f, 25.f, 8.f, 17.f, -1.f, 12.f, 7.f);
        heart.CloseFigure();
        if (icon == Icon::HeartFilled)
            g.FillPath(&brush, &heart);
        else
            g.DrawPath(&pen, &heart);
        break;
    }
    default:
        break;
    }
    g.Restore(state);
}
} // namespace
void drawIcon(HDC dc, Icon icon, float cx, float cy, float size, COLORREF color)
{
    RECT bounds{LONG(std::floor(cx - size / 2)), LONG(std::floor(cy - size / 2)),
                LONG(std::ceil(cx + size / 2)), LONG(std::ceil(cy + size / 2))};
    if (!RectVisible(dc, &bounds))
        return;
    Gdiplus::Graphics graphics(dc);
    render(graphics, icon, cx, cy, size, color);
}
void drawAppMark(HDC dc, float left, float top, float size)
{
    Gdiplus::Graphics g(dc);
    g.SetSmoothingMode(Gdiplus::SmoothingModeAntiAlias);
    Gdiplus::SolidBrush bg(Gdiplus::Color(255, 8, 10, 9));
    g.FillRectangle(&bg, left, top, size, size);
    Gdiplus::Matrix matrix(size / 108, 0, 0, size / 108, left, top);
    g.SetTransform(&matrix);
    Gdiplus::PointF points[]{{22, 56}, {35, 56}, {43, 39}, {53, 72}, {64, 29}, {73, 56}, {86, 56}};
    Gdiplus::Pen pen(Gdiplus::Color(255, 221, 230, 226), 7);
    pen.SetLineJoin(Gdiplus::LineJoinRound);
    pen.SetStartCap(Gdiplus::LineCapRound);
    pen.SetEndCap(Gdiplus::LineCapRound);
    g.DrawLines(&pen, points, 7);
    Gdiplus::SolidBrush accent(Gdiplus::Color(255, 139, 233, 211));
    g.FillEllipse(&accent, 62.f, 51.f, 10.f, 10.f);
}
bool iconTests(const std::filesystem::path &directory)
{
    ULONG_PTR token = 0;
    Gdiplus::GdiplusStartupInput startup;
    if (Gdiplus::GdiplusStartup(&token, &startup, nullptr) != Gdiplus::Ok)
        return false;
    bool passed = true;
    {
        UINT count = 0, bytes = 0;
        Gdiplus::GetImageEncodersSize(&count, &bytes);
        std::vector<BYTE> data(bytes);
        auto encoders = reinterpret_cast<Gdiplus::ImageCodecInfo *>(data.data());
        Gdiplus::GetImageEncoders(count, bytes, encoders);
        CLSID png{};
        for (UINT i = 0; i < count; ++i)
            if (wcscmp(encoders[i].MimeType, L"image/png") == 0)
                png = encoders[i].Clsid;
        std::ofstream report(directory / L"vector-icons.txt");
        for (int dpi : {96, 144, 192, 288, 384})
        {
            int cell = 48 * dpi / 96, size = 24 * dpi / 96;
            Gdiplus::Bitmap sheet(cell * 8, cell * 4, PixelFormat32bppARGB);
            Gdiplus::Graphics graphics(&sheet);
            graphics.Clear(Gdiplus::Color(255, 19, 19, 21));
            for (int i = 0; i < int(Icon::Count); ++i)
            {
                Gdiplus::Bitmap image(cell, cell, PixelFormat32bppARGB);
                {
                    Gdiplus::Graphics g(&image);
                    g.Clear(Gdiplus::Color(0, 0, 0, 0));
                    render(g, Icon(i), float(cell) / 2, float(cell) / 2, float(size), RGB(235, 238, 236));
                }
                int ink = 0, partial = 0, edge = 0;
                for (int y = 0; y < cell; ++y)
                    for (int x = 0; x < cell; ++x)
                    {
                        Gdiplus::Color pixel;
                        image.GetPixel(x, y, &pixel);
                        if (pixel.GetA())
                        {
                            ++ink;
                            if (x == 0 || y == 0 || x == cell - 1 || y == cell - 1)
                                ++edge;
                        }
                        if (pixel.GetA() && pixel.GetA() < 255)
                            ++partial;
                    }
                // Check every actual drawing, not source-code strings or font availability.
                bool ok = ink > size && partial > 0 && edge == 0;
                report << (ok ? "PASS " : "FAIL ") << "dpi=" << dpi << " icon=" << i << " ink=" << ink
                       << " antialiased=" << partial << " clipped=" << edge << '\n';
                passed = passed && ok;
                graphics.DrawImage(&image, i % 8 * cell, i / 8 * cell);
            }
            auto path = directory / (L"vector-icons-" + std::to_wstring(dpi) + L"dpi.png");
            passed = sheet.Save(path.c_str(), &png, nullptr) == Gdiplus::Ok && passed;
        }
    }
    Gdiplus::GdiplusShutdown(token);
    return passed;
}
} // namespace ga
