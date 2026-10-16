#pragma once
#include <windows.h>
#include <filesystem>

namespace ga
{
enum class Icon
{
    Play,
    Pause,
    Previous,
    Next,
    Repeat,
    RepeatOne,
    Library,
    Stack,
    Nodes,
    Settings,
    Refresh,
    History,
    More,
    Plus,
    Close,
    Search,
    Sort,
    Folder,
    Save,
    Expand,
    Collapse,
    Home,
    Lock,
    Unlock,
    Share,
    Video,
    Music,
    Magic,
    Fit,
    Heart,
    HeartFilled,
    Count
};
// Original 24-unit paths, rasterized at the destination's physical DPI. No font or bitmap fallback.
void drawIcon(HDC dc, Icon icon, float centerX, float centerY, float pixels, COLORREF color);
void drawAppMark(HDC dc, float left, float top, float pixels);
bool iconTests(const std::filesystem::path &directory);
} // namespace ga
