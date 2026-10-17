#define NOMINMAX
#include "AudioAnalysis.h"
#include "AmbientMesh.h"
#include "AudioFallback.h"
#include "CodecReader.h"
#include "Core.h"
#include "Crash.h"
#include "Features.h"
#include "FrameClock.h"
#include "GraphMotion.h"
#include "GraphMotionTests.h"
#include "Icons.h"
#include "Language.h"
#include "Media.h"
#include "WorkQueue.h"
#include <future>
#include <SystemMediaTransportControlsInterop.h>
#include <array>
#include <atomic>
#include <chrono>
#include <commctrl.h>
#include <dwmapi.h>
#include <gdiplus.h>
#include <iomanip>
#include <mutex>
#include <oleacc.h>
#include <propkey.h>
#include <propvarutil.h>
#include <random>
#include <shellapi.h>
#include <shlobj.h>
#include <shobjidl.h>
#include <shobjidl_core.h>
#include <sstream>
#include <thread>
#include <windowsx.h>
#include <winrt/Windows.ApplicationModel.DataTransfer.h>
#include <winrt/Windows.Foundation.Collections.h>
#include <winrt/Windows.Foundation.h>
#include <winrt/Windows.Media.h>
#include <winrt/Windows.Storage.h>
#include <wrl/client.h>
#ifdef small
#undef small
#endif

using namespace ga;
using Microsoft::WRL::ComPtr;
namespace
{
constexpr UINT SCANNED = WM_APP + 1, GRAPH_READY = WM_APP + 2, ALIGN_READY = WM_APP + 8,
               ASSET_READY = WM_APP + 9, TASK_FAILURE = WM_APP + 26, VISUAL_FRAME = WM_APP + 27;
constexpr int SEARCH = 101, GRAPH_SEARCH = 102, PICKER_SEARCH = 103;
struct Rect
{
    int x = 0, y = 0, w = 0, h = 0;
    bool contains(int px, int py) const
    {
        return px >= x && py >= y && px < x + w && py < y + h;
    }
};
enum Action
{
    NONE,
    ADD_FOLDER,
    OPEN_SETTINGS,
    REFRESH,
    LIBRARY_TAB,
    SETTINGS_TAB,
    TOGGLE_STACK,
    TOGGLE_NODES,
    ALL_FILES,
    FAVORITES,
    SORT,
    CLEAR_SEARCH,
    PLAY_SONG,
    FAVORITE_SONG,
    ADD_SONG,
    ADD_STACK,
    SAVE_STACK,
    LOAD_STACK,
    MUTE_SLOT,
    SOLO_SLOT,
    REMOVE_SLOT,
    GAIN_SLOT,
    PLAY_PAUSE,
    PREVIOUS,
    NEXT,
    REPEAT,
    SPEED,
    SEEK,
    MASTER_GAIN,
    EXPAND,
    HOME,
    SHRINK,
    CLOSE_PLAYER,
    FULLSCREEN,
    FIT_GRAPH,
    SCATTER_GRAPH,
    GRAPH_CONTROLS,
    PALETTE,
    ROW_SIZE,
    MINI_SIZE,
    SETTINGS_SECTION,
    REMOVE_FOLDER,
    OPEN_SOURCE_MENU,
    TOOLS,
    HISTORY_MENU,
    PLAYLIST_MENU,
    TRANSPORT_LOCK,
    ALIGN_SOUND,
    OFFSET_LESS,
    OFFSET_MORE,
    OFFSET_RESET,
    OFFSET_OPTIONS,
    SHARE_FILES
};
struct Hit
{
    Rect box;
    Action action = NONE;
    int data = 0;
};
struct Slot
{
    Song song;
    void *player = nullptr;
    int gain = 100;
    bool mute = false, solo = false;
    double offset = 0;
};
struct Palette
{
    COLORREF bg, rail, panel, raised, text, muted, line, accent, active;
};
Palette colors{RGB(24, 24, 26),    RGB(19, 19, 21), RGB(28, 28, 30),    RGB(36, 36, 39), RGB(235, 238, 236),
               RGB(160, 169, 164), RGB(52, 55, 54), RGB(131, 212, 185), RGB(30, 61, 50)};
struct App
{
    HWND main = nullptr, player = nullptr, mini = nullptr, preview = nullptr, search = nullptr,
         nodeSearch = nullptr, picker = nullptr, pickerSearch = nullptr;
    Settings settings;
    UserFeatures features;
    Practice practice;
    bool locked = false, lyricsPanel = false, aligning = false;
    bool exiting = false;
    FrameClock frameClock;
    BOOL motionEnabled = TRUE;
    std::uint64_t visualFrames = 0, visualPaints = 0, lastPlaybackPoll = 0;
    int keyboardFocus = -1;
    std::wstring tapPath;
    std::uint64_t tapAt = 0;
    std::uint64_t titleAt = 0, playbackReadAt = 0;
    bool barrier = false;
    std::uint64_t barrierAt = 0;
    std::uint64_t sleepUntil = 0, syncAt = 0, holdAt = 0;
    HWND details = nullptr;
    std::vector<Lyric> lyrics;
    std::vector<std::wstring> queue;
    std::wstring activePlaylist;
    double temporarySpeed = 0;
    SavedStack pendingStackSave;
    std::filesystem::path dataDir;
    std::vector<Song> songs;
    Graph graph;
    GraphMotion graphMotion;
    unsigned graphEpoch = 0;
    bool graphAwake = false;
    int graphHover = -1;
    Point graphPinned{};
    std::uint64_t graphFrameAt = 0, graphRevealAt = 0;
    double graphTravel = 0;
    double graphAge = 0;
    std::array<double, 4> graphPaintCost{};
    std::array<double, 5> mainPaintParts{};
    double mainPaintCost = 0;
    std::uint64_t graphPaintCount = 0;
    std::vector<int> filtered;
    std::vector<Slot> slots;
    std::unique_ptr<AudioFallback> stackOutput;
    bool stackPending = false, stackWanted = false, loadingStack = false;
    double stackTarget = 0;
    MediaState playback;
    std::vector<Hit> mainHits, playerHits, miniHits, pickerHits;
    std::wstring query, nodeQuery, pickerQuery, status;
    WorkQueue scans;
    std::shared_ptr<Gdiplus::Bitmap> artwork;
    std::shared_ptr<Gdiplus::Bitmap> customBackground;
    std::vector<float> waveform;
    float wavePeak = 0;
    std::atomic<bool> assetPermit{false};
    std::wstring artist, album;
    COLORREF ambient[2]{RGB(60, 110, 95), RGB(38, 65, 96)};
    COLORREF artworkColors[2]{RGB(60, 110, 95), RGB(38, 65, 96)};
    COLORREF ambientFrom[2]{RGB(60, 110, 95), RGB(38, 65, 96)},
        ambientTarget[2]{RGB(60, 110, 95), RGB(38, 65, 96)};
    std::uint64_t ambientAt = 0, ambientGeneration = 0, ambientSamples = 0;
    void *ambientPlayer = nullptr;
    bool ambientEnabled = false;
    float ambientBrightness = .15f, ambientSaturation = .3f;
    std::atomic<unsigned> assetGeneration{0};
    std::mutex assetMutex;
    struct AssetResult
    {
        unsigned generation = 0;
        std::shared_ptr<Gdiplus::Bitmap> artwork;
        std::vector<float> waveform;
        std::wstring artist, album;
        COLORREF ambient[2]{RGB(60, 110, 95), RGB(38, 65, 96)};
    };
    std::vector<AssetResult> assetResults;
    std::atomic<unsigned> alignmentGeneration{0};
    std::mutex alignmentMutex;
    struct AlignmentResult
    {
        unsigned generation;
        std::vector<std::wstring> paths;
        std::vector<AudioMatch> matches;
        std::wstring error;
    };
    std::vector<AlignmentResult> alignments;
    std::atomic<unsigned> generation{0};
    std::atomic<bool> closing{false};
    std::mutex resultsMutex;
    struct Result
    {
        unsigned generation = 0;
        std::vector<Song> songs;
        Graph graph;
    };
    std::vector<Result> results;
    HFONT small = nullptr, body = nullptr, title = nullptr, heading = nullptr;
    HBRUSH editBrush = nullptr;
    HICON icon = nullptr;
    int dpi = 96, clientW = 1360, clientH = 820, libraryScroll = 0, stackScroll = 0, pickerScroll = 0,
        settingsScroll = 0, master = 80, hover = -1;
    bool showStack = true, showNodes = true, favoritesOnly = false, settingsPage = false, fullscreen = false,
         busy = false, searchFocused = false;
    std::array<bool, 4> section{false, false, false, false};
    Rect libraryRect, stackRect, nodesRect, timeline, previewRect;
    Action drag = NONE;
    int dragSlot = 0, dragStartX = 0, dragStartY = 0;
    double startLeft = 0, startRight = 0;
    double speed = 1, zoom = 1, panX = 0, panY = 0;
    int graphDrag = -1;
    bool graphMoved = false, graphSpace = false, graphFocused = false, graphMiddle = false;
    Point graphPointer{};
    double graphMinX = 0, graphMaxX = 1, graphMinY = 0, graphMaxY = 1;
    std::uint64_t now = 0, statusUntil = 0;
    int previousState = 0;
    WINDOWPLACEMENT placement{sizeof(WINDOWPLACEMENT)};
    bool probe = false;
    bool probeAmbient = false;
    bool probeFault = false;
    bool probeVisual = false;
    bool probeGraph = false;
    std::uint64_t probeCpu = 0;
    void *probePlayer = nullptr;
    std::uint64_t probeSamples = 0;
    double probeTime = 0;
    std::wstring probeFixture;
    std::filesystem::path probeOutput;
    int probeStage = 0;
    std::uint64_t probeAt = 0;
    bool probeFailed = false;
} app;
void requestAssets(const Song &);
void loadCustomBackground();
void seek(double);
Point compactMetrics()
{
    double height = 80 + app.settings.miniSize * 16;
    double ratio = app.playback.width && app.playback.height
                       ? double(app.playback.width) / app.playback.height
                       : (!app.slots.empty() && app.slots[0].song.video ? 16.0 / 9 : 1.0);
    if (app.playback.width == 0 && app.artwork && app.artwork->GetHeight())
        ratio = double(app.artwork->GetWidth()) / app.artwork->GetHeight();
    double width = height * ratio;
    if (width > 320)
    {
        width = 320;
        height = width / ratio;
    }
    return {std::max(48.0, width), std::max(24.0, height)};
}
int dockHeight()
{
    return std::max(116, int(compactMetrics().y) + 20);
}
int px(int value)
{
    return MulDiv(value, app.dpi, 96);
}
int logical(int value)
{
    return MulDiv(value, 96, app.dpi);
}
RECT physical(Rect r)
{
    return {px(r.x), px(r.y), px(r.x + r.w), px(r.y + r.h)};
}
void fill(HDC dc, Rect rect, COLORREF color)
{
    auto r = physical(rect);
    if (!RectVisible(dc, &r))
        return;
    if (app.features.background == 3 && app.customBackground && !app.features.highContrast &&
        (color == colors.panel || color == colors.bg))
    {
        Gdiplus::Graphics graphics(dc);
        int state = graphics.Save();
        graphics.SetClip(Gdiplus::Rect(r.left, r.top, r.right - r.left, r.bottom - r.top));
        auto image = app.customBackground.get();
        double sx = double(px(app.clientW)) / image->GetWidth(),
               sy = double(px(app.clientH)) / image->GetHeight();
        double scale = app.features.scale ? std::min(sx, sy) : std::max(sx, sy);
        int width = int(image->GetWidth() * scale), height = int(image->GetHeight() * scale);
        auto brush = CreateSolidBrush(color);
        FillRect(dc, &r, brush);
        DeleteObject(brush);
        graphics.DrawImage(image, (px(app.clientW) - width) / 2, (px(app.clientH) - height) / 2, width,
                           height);
        Gdiplus::SolidBrush dim(Gdiplus::Color(190, GetRValue(color), GetGValue(color), GetBValue(color)));
        graphics.FillRectangle(
            &dim, Gdiplus::Rect(int(r.left), int(r.top), int(r.right - r.left), int(r.bottom - r.top)));
        graphics.Restore(state);
        return;
    }
    if (app.features.background > 0 && !app.features.highContrast &&
        (color == colors.panel || color == colors.bg))
    {
        if (app.features.background == 2)
        {
            double phase = app.motionEnabled && app.playback.state == 3 ? app.now / 19000.0 : 0;
            static AmbientLayer layers[2];
            layers[color == colors.bg ? 0 : 1].paint(
                dc, r, std::max(px(app.clientW), int(r.right)), std::max(px(app.clientH), int(r.bottom)),
                color, app.ambient, app.ambientBrightness, app.ambientSaturation, phase);
            return;
        }
        auto mix = [](COLORREF base, COLORREF tint) {
            return RGB((GetRValue(base) * 4 + GetRValue(tint)) / 5,
                       (GetGValue(base) * 4 + GetGValue(tint)) / 5,
                       (GetBValue(base) * 4 + GetBValue(tint)) / 5);
        };
        auto top = mix(color, app.ambient[0]), bottom = mix(color, app.ambient[1]);
        TRIVERTEX vertices[2]{{r.left, r.top, COLOR16(GetRValue(top) << 8), COLOR16(GetGValue(top) << 8),
                               COLOR16(GetBValue(top) << 8), 0},
                              {r.right, r.bottom, COLOR16(GetRValue(bottom) << 8),
                               COLOR16(GetGValue(bottom) << 8), COLOR16(GetBValue(bottom) << 8), 0}};
        GRADIENT_RECT gradient{0, 1};
        GradientFill(dc, vertices, 2, &gradient, 1, GRADIENT_FILL_RECT_V);
        return;
    }
    auto brush = CreateSolidBrush(color);
    FillRect(dc, &r, brush);
    DeleteObject(brush);
}
void line(HDC dc, int x1, int y1, int x2, int y2, COLORREF color, int width = 1)
{
    auto pen = CreatePen(PS_SOLID, px(width), color);
    auto old = SelectObject(dc, pen);
    MoveToEx(dc, px(x1), px(y1), nullptr);
    LineTo(dc, px(x2), px(y2));
    SelectObject(dc, old);
    DeleteObject(pen);
}
void text(HDC dc, std::wstring value, Rect rect, HFONT font, COLORREF color,
          UINT flags = DT_LEFT | DT_VCENTER | DT_SINGLELINE | DT_END_ELLIPSIS)
{
    auto r = physical(rect);
    // GDI still shapes clipped text. Skip it before font/fallback layout; graph
    // motion must not re-layout every title in an unchanged Library or Stack.
    if (!RectVisible(dc, &r))
        return;
    auto old = SelectObject(dc, font);
    SetTextColor(dc, color);
    SetBkMode(dc, TRANSPARENT);
    DrawText(dc, value.c_str(), int(value.size()), &r, flags);
    SelectObject(dc, old);
}
void circle(HDC dc, int x, int y, int radius, COLORREF color)
{
    auto brush = CreateSolidBrush(color);
    auto oldBrush = SelectObject(dc, brush), oldPen = SelectObject(dc, GetStockObject(NULL_PEN));
    Ellipse(dc, px(x - radius), px(y - radius), px(x + radius), px(y + radius));
    SelectObject(dc, oldBrush);
    SelectObject(dc, oldPen);
    DeleteObject(brush);
}
void marquee(HDC dc, const std::wstring &value, Rect rect, HFONT font, COLORREF color)
{
    auto clip = physical(rect);
    if (!RectVisible(dc, &clip))
        return;
    SIZE size{};
    auto previous = SelectObject(dc, font);
    GetTextExtentPoint32(dc, value.c_str(), int(value.size()), &size);
    SelectObject(dc, previous);
    if (size.cx <= px(rect.w) || !app.motionEnabled)
    {
        text(dc, value, rect, font, color);
        return;
    }
    Gdiplus::Graphics graphics(dc);
    graphics.SetTextRenderingHint(Gdiplus::TextRenderingHintAntiAliasGridFit);
    graphics.SetClip(Gdiplus::Rect(clip.left, clip.top, clip.right - clip.left, clip.bottom - clip.top));
    Gdiplus::Font face(dc, font);
    Gdiplus::StringFormat format(Gdiplus::StringFormat::GenericTypographic());
    format.SetFormatFlags(format.GetFormatFlags() | Gdiplus::StringFormatFlagsNoWrap |
                          Gdiplus::StringFormatFlagsMeasureTrailingSpaces);
    Gdiplus::RectF measured;
    graphics.MeasureString(value.c_str(), int(value.size()), &face, Gdiplus::PointF(0, 0), &format,
                           &measured);
    double seconds = std::max(0.0, double(GetTickCount64() - app.titleAt) / 1000 - 1.5);
    float cycle = measured.Width + 36 * app.dpi / 96.f;
    float offset = float(std::fmod(seconds * 20 * app.dpi / 96, cycle));
    float top = clip.top + (clip.bottom - clip.top - face.GetHeight(&graphics)) / 2;
    Gdiplus::SolidBrush brush(Gdiplus::Color(255, GetRValue(color), GetGValue(color), GetBValue(color)));
    for (float x : {clip.left - offset, clip.left - offset + cycle})
        graphics.DrawString(value.c_str(), int(value.size()), &face, Gdiplus::PointF(x, top), &format,
                            &brush);
}
void mark(HDC dc, Rect r)
{
    auto clip = physical(r);
    if (!RectVisible(dc, &clip))
        return;
    float scale = app.dpi / 96.f;
    drawAppMark(dc, r.x * scale, r.y * scale, r.w * scale);
}
void uiIcon(HDC dc, Icon icon, Rect r, COLORREF color, float size = 24)
{
    float scale = app.dpi / 96.f;
    drawIcon(dc, icon, (r.x + r.w / 2.f) * scale, (r.y + r.h / 2.f) * scale, size * scale, color);
}
void fonts()
{
    for (auto font : {app.small, app.body, app.title, app.heading})
        if (font)
            DeleteObject(font);
    auto make = [](int size, int weight, const wchar_t *name) {
        return CreateFont(-px(size), 0, 0, 0, weight, FALSE, FALSE, FALSE, DEFAULT_CHARSET,
                          OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY, DEFAULT_PITCH, name);
    };
    const wchar_t *faces[]{L"Segoe UI", L"Arial", L"Georgia", L"Consolas"};
    const wchar_t *face = faces[std::clamp(app.features.font, 0, 3)];
    app.small = make(12, FW_NORMAL, face);
    app.body = make(14, FW_NORMAL, face);
    app.title = make(15, FW_SEMIBOLD, face);
    app.heading = make(23, FW_SEMIBOLD, face);
}
void theme()
{
    if (app.settings.palette == 1)
        colors = {RGB(22, 25, 30), RGB(17, 20, 25),    RGB(27, 31, 37),
                  RGB(37, 43, 51), RGB(233, 237, 241), RGB(162, 174, 185),
                  RGB(55, 63, 72), RGB(174, 201, 223), RGB(35, 58, 77)};
    else if (app.settings.palette == 2)
        colors = {RGB(19, 19, 20), RGB(14, 14, 15),    RGB(25, 25, 27),
                  RGB(36, 36, 38), RGB(235, 237, 239), RGB(169, 171, 176),
                  RGB(56, 57, 60), RGB(216, 218, 220), RGB(48, 49, 52)};
    else
        colors = {RGB(24, 24, 26), RGB(19, 19, 21),    RGB(28, 28, 30),
                  RGB(36, 36, 39), RGB(235, 238, 236), RGB(160, 169, 164),
                  RGB(52, 55, 54), RGB(131, 212, 185), RGB(30, 61, 50)};
    if (app.features.light)
        colors = {RGB(244, 245, 245), RGB(233, 236, 235), RGB(250, 251, 250),
                  RGB(232, 235, 234), RGB(22, 28, 25),    RGB(73, 83, 79),
                  RGB(191, 201, 196), RGB(21, 107, 83),   RGB(202, 232, 220)};
    if (app.features.highContrast)
        colors = {RGB(0, 0, 0),       RGB(0, 0, 0),       RGB(0, 0, 0),
                  RGB(0, 0, 0),       RGB(255, 255, 255), RGB(255, 255, 255),
                  RGB(255, 255, 255), RGB(255, 255, 0),   RGB(30, 30, 30)};
    if (app.editBrush)
        DeleteObject(app.editBrush);
    app.editBrush = CreateSolidBrush(colors.raised);
}
void invalidate()
{
    if (app.main)
        InvalidateRect(app.main, nullptr, FALSE);
    if (app.player)
        InvalidateRect(app.player, nullptr, FALSE);
    if (app.mini)
        InvalidateRect(app.mini, nullptr, FALSE);
    if (app.picker)
        InvalidateRect(app.picker, nullptr, FALSE);
}
void toast(const std::wstring &message)
{
    app.status = message;
    app.statusUntil = GetTickCount64() + 5000;
    invalidate();
}
void persist()
{
    app.features.speed = app.speed;
    if (!saveSettings(app.dataDir, app.settings) || !saveFeatures(app.dataDir, app.features))
        toast(L"Settings could not be saved. Check folder access.");
}
void filter()
{
    app.filtered.clear();
    auto q = lower(app.query);
    for (std::size_t i = 0; i < app.songs.size(); ++i)
        if ((q.empty() || lower(app.songs[i].title).find(q) != std::wstring::npos) &&
            (!app.favoritesOnly || app.settings.favorites.count(app.songs[i].path)))
            if (app.activePlaylist.empty() ||
                std::find(app.queue.begin(), app.queue.end(), app.songs[i].path) != app.queue.end())
                app.filtered.push_back(int(i));
    std::sort(app.filtered.begin(), app.filtered.end(), [](int a, int b) {
        if (!app.activePlaylist.empty())
            return std::find(app.queue.begin(), app.queue.end(), app.songs[a].path) <
                   std::find(app.queue.begin(), app.queue.end(), app.songs[b].path);
        if (app.settings.sort == 0)
            return app.songs[a].modified > app.songs[b].modified;
        if (app.settings.sort == 1)
            return app.songs[a].modified < app.songs[b].modified;
        return lower(app.songs[a].title) < lower(app.songs[b].title);
    });
    app.libraryScroll = std::clamp(app.libraryScroll, 0, std::max(0, int(app.filtered.size()) - 1));
    invalidate();
}
void scan()
{
    app.busy = true;
    unsigned generation = ++app.generation;
    auto roots = app.settings.roots;
    int neighbors = app.features.graphNeighbors;
    double minimum = app.features.graphThreshold / 100.0;
    invalidate();
    bool accepted = app.scans.emplace_back(
        [generation, roots, neighbors, minimum] {
            SetThreadPriority(GetCurrentThread(), THREAD_PRIORITY_BELOW_NORMAL);
            auto canceled = [generation] { return app.closing || app.generation != generation; };
            auto songs = scanLibrary(roots, canceled);
            if (canceled())
                return;
            Graph graph;
            bool cached = loadGraphCache(app.dataDir, songs, graph, neighbors, minimum);
            if (!cached)
                graph = buildGraph(songs, canceled, neighbors, minimum);
            if (canceled())
                return;
            {
                std::lock_guard<std::mutex> guard(app.resultsMutex);
                if (canceled())
                    return;
                if (!cached)
                    saveGraphCache(app.dataDir, songs, graph, neighbors, minimum);
                app.results.push_back({generation, std::move(songs), std::move(graph)});
            }
            PostMessage(app.main, SCANNED, 0, 0);
        },
        [generation] {
            if (!app.closing)
                PostMessage(app.main, TASK_FAILURE, 1, LPARAM(generation));
        });
    if (!accepted)
    {
        app.busy = false;
        toast(L"Background work is busy. Refresh again when it finishes.");
    }
}
std::vector<PcmVoice> pcmVoices()
{
    bool solo = std::any_of(app.slots.begin(), app.slots.end(), [](auto &slot) { return slot.solo; });
    std::vector<PcmVoice> voices;
    for (auto &slot : app.slots)
        voices.push_back(
            {slot.song.path, mixedGain(100, slot.gain, slot.mute, slot.solo, solo) / 100.0, slot.offset});
    return voices;
}
void gains()
{
    if (app.stackOutput)
    {
        app.stackOutput->voices(pcmVoices());
        app.stackOutput->volume(app.master / 100.0);
        return;
    }
    if (app.slots.size() > 1)
    {
        mediaVolume(app.slots[0].player, 0);
        return;
    }
    bool solo = std::any_of(app.slots.begin(), app.slots.end(), [](auto &slot) { return slot.solo; });
    for (auto &slot : app.slots)
        if (slot.player)
            mediaVolume(slot.player, mixedGain(app.master, slot.gain, slot.mute, slot.solo, solo) / 100.0);
}
void rebuildStackOutput()
{
    if (app.loadingStack || app.slots.empty())
        return;
    bool playing = app.stackOutput
                       ? (app.stackPending ? app.stackWanted : app.stackOutput->state().state == 3)
                       : (app.playback.state == 3 || app.barrier || app.stackWanted);
    double position = app.stackOutput ? (app.stackPending ? app.stackTarget : app.stackOutput->state().time)
                                      : app.playback.time;
    app.stackTarget = position;
    if (app.stackOutput)
        app.stackOutput->pause(true);
    // The native primary remains the video presentation, never a second audible
    // singer. Companions have no Media Engine/audio endpoint at all.
    mediaPause(app.slots[0].player, true);
    app.stackOutput.reset();
    app.barrier = false;
    app.stackWanted = playing;
    if (app.slots.size() == 1)
    {
        app.stackPending = false;
        mediaSeek(app.slots[0].player, position);
        gains();
        mediaPause(app.slots[0].player, !playing);
        return;
    }
    mediaVolume(app.slots[0].player, 0);
    try
    {
        app.stackOutput = std::make_unique<AudioFallback>(pcmVoices());
    }
    catch (...)
    {
        app.stackPending = app.stackWanted = false;
        toast(L"Stack output could not start. Remove a voice or reload the Stack to retry.");
        return;
    }
    app.stackOutput->loop(app.settings.loop);
    app.stackOutput->rate(app.speed);
    app.stackOutput->seek(position);
    app.stackPending = true;
    gains();
}
MediaState playbackSnapshot()
{
    if (app.slots.empty())
        return {};
    auto presentation = mediaState(app.slots[0].player);
    if (!app.stackOutput)
    {
        if (app.slots.size() > 1)
            presentation.state = 7;
        return presentation;
    }
    auto audio = app.stackOutput->state();
    bool videoFailed = app.slots[0].song.video && presentation.state == 7;
    presentation.time = app.stackPending ? app.stackTarget : audio.time;
    if (audio.duration > 0)
        presentation.duration = audio.duration;
    presentation.state = videoFailed ? 7 : app.stackPending && audio.state != 7 ? 1 : audio.state;
    presentation.sharedClock = true;
    presentation.pcmRate = audio.pcmRate;
    presentation.pcmChannels = audio.pcmChannels;
    presentation.pcmUnderruns = audio.pcmUnderruns;
    return presentation;
}
void releaseSlots()
{
    ++app.assetGeneration;
    ++app.alignmentGeneration;
    app.aligning = false;
    app.stackOutput.reset();
    app.stackPending = app.stackWanted = false;
    app.stackTarget = 0;
    for (auto &slot : app.slots)
        if (slot.player)
            mediaRelease(slot.player);
    app.slots.clear();
    app.playback = {};
    app.previousState = 0;
    app.barrier = false;
}
void positionPreview(HWND parent, Rect rect)
{
    if (!app.preview || !parent)
        return;
    app.previewRect = rect;
    if (GetParent(app.preview) != parent)
        SetParent(app.preview, parent);
    MoveWindow(app.preview, px(rect.x), px(rect.y), px(rect.w), px(rect.h), TRUE);
    ShowWindow(app.preview, SW_SHOWNA);
    if (!app.slots.empty())
        mediaAttach(app.slots[0].player, app.preview);
}
void playSong(const Song &input)
{
    Song song = input;
    song.path = canonicalPath(song.path);
    releaseSlots();
    app.practice = {};
    app.titleAt = GetTickCount64();
    app.lyrics = parseLyrics(readLocalText(std::filesystem::path(song.path).replace_extension(L".lrc")));
    void *player = mediaCreate(false);
    if (!player)
    {
        toast(L"Windows media services could not start.");
        return;
    }
    app.slots.push_back({song, player, 100, false, false});
    mediaAttach(player, app.preview);
    HRESULT result = mediaLoad(player, song.path);
    if (SUCCEEDED(result))
        result = mediaPlay(player);
    if (FAILED(result))
    {
        toast(L"This file cannot be opened by Windows media services.");
        releaseSlots();
    }
    else
    {
        app.stackWanted = true;
        mediaRate(player, app.speed);
        gains();
        app.features.lastPath = song.path;
        app.features.lastPosition = 0;
        recordHistory(app.features, song.path, std::int64_t(std::time(nullptr)));
        persist();
        requestAssets(song);
    }
    invalidate();
}
void addSong(const Song &song)
{
    if (app.slots.empty())
    {
        playSong(song);
        return;
    }
    if (app.slots.size() >= 8)
    {
        toast(L"Stack holds up to eight tracks.");
        return;
    }
    if (!codecsAvailable() || !localMediaPath(song.path))
    {
        toast(L"Stack needs the bundled local audio codecs. No independent-player fallback is used.");
        return;
    }
    Song normalized = song;
    normalized.path = canonicalPath(song.path);
    app.slots.push_back({normalized, nullptr, 70, false, false});
    rebuildStackOutput();
    invalidate();
}
void toggle()
{
    if (app.slots.empty())
        return;
    if (app.slots.size() > 1 && !app.stackOutput)
    {
        toast(L"Stack output is unavailable. Remove a voice or reload to retry.");
        return;
    }
    if (app.stackOutput)
    {
        if (!app.stackPending && app.stackOutput->state().state == 6)
        {
            seek(0);
            app.stackWanted = true;
            return;
        }
        bool pause = app.stackWanted;
        app.stackWanted = !pause;
        if (!app.stackPending)
        {
            app.stackOutput->pause(pause);
            mediaPause(app.slots[0].player, pause);
        }
        return;
    }
    bool pause = app.playback.state == 3 || app.barrier;
    app.stackWanted = !pause;
    app.barrier = false;
    for (auto &slot : app.slots)
        if (slot.player)
            mediaPause(slot.player, pause);
}
void seek(double position)
{
    position = std::clamp(position, 0.0, std::max(.01, app.playback.duration));
    if (app.stackOutput)
    {
        app.stackWanted = app.stackPending ? app.stackWanted : app.stackOutput->state().state == 3;
        app.stackOutput->pause(true);
        mediaPause(app.slots[0].player, true);
        app.stackOutput->voices(pcmVoices());
        app.stackTarget = position;
        app.stackOutput->seek(position);
        app.stackPending = true;
        return;
    }
    for (auto &slot : app.slots)
        if (slot.player && SUCCEEDED(mediaSeek(slot.player, std::max(0.0, position + slot.offset))))
        {
            app.playback.time = position;
            app.playbackReadAt = GetTickCount64();
        }
}
void next(int direction)
{
    if (app.filtered.empty())
        return;
    int current = -1;
    if (!app.slots.empty())
        for (std::size_t i = 0; i < app.filtered.size(); ++i)
            if (app.songs[app.filtered[i]].path == app.slots[0].song.path)
            {
                current = int(i);
                break;
            }
    int index = (current + direction + int(app.filtered.size())) % int(app.filtered.size());
    if (app.features.shuffle && app.filtered.size() > 1)
    {
        static std::mt19937 random{std::random_device{}()};
        index = std::uniform_int_distribution<int>(0, int(app.filtered.size()) - 2)(random);
        if (index >= current)
            ++index;
    }
    playSong(app.songs[app.filtered[index]]);
}
void favorite(int index)
{
    if (index < 0 || index >= int(app.songs.size()))
        return;
    auto path = app.songs[index].path;
    if (!app.settings.favorites.erase(path))
        app.settings.favorites.insert(path);
    persist();
    filter();
}
std::wstring bytes(const Song &song)
{
    wchar_t value[48]{};
    swprintf_s(value, L"%.1f MB", song.bytes / 1048576.0);
    return value;
}
bool primary(const Song &song)
{
    return !app.slots.empty() && app.slots[0].song.path == song.path;
}
void clipboard(const std::wstring &value)
{
    if (!OpenClipboard(app.main))
        return;
    EmptyClipboard();
    auto memory = GlobalAlloc(GMEM_MOVEABLE, (value.size() + 1) * 2);
    if (memory)
    {
        auto data = GlobalLock(memory);
        if (data)
        {
            memcpy(data, value.c_str(), (value.size() + 1) * 2);
            GlobalUnlock(memory);
            if (!SetClipboardData(CF_UNICODETEXT, memory))
                GlobalFree(memory);
        }
        else
            GlobalFree(memory);
    }
    CloseClipboard();
    toast(L"Copied to clipboard.");
}
std::wstring chooseFolder()
{
    ComPtr<IFileOpenDialog> dialog;
    if (FAILED(CoCreateInstance(CLSID_FileOpenDialog, nullptr, CLSCTX_INPROC_SERVER, IID_PPV_ARGS(&dialog))))
        return {};
    dialog->SetOptions(FOS_PICKFOLDERS | FOS_FORCEFILESYSTEM | FOS_PATHMUSTEXIST | FOS_NOCHANGEDIR);
    if (FAILED(dialog->Show(app.main)))
        return {};
    ComPtr<IShellItem> item;
    if (FAILED(dialog->GetResult(&item)))
        return {};
    PWSTR path = nullptr;
    if (FAILED(item->GetDisplayName(SIGDN_FILESYSPATH, &path)))
        return {};
    std::wstring result(path);
    CoTaskMemFree(path);
    return result;
}
void addFolder()
{
    auto root = chooseFolder();
    if (root.empty())
        return;
    if (root.rfind(L"\\\\", 0) == 0)
    {
        toast(L"Choose a local folder. Network folders are not enabled.");
        return;
    }
    if (app.settings.roots.size() >= 128)
    {
        toast(L"The folder limit is 128.");
        return;
    }
    if (std::find(app.settings.roots.begin(), app.settings.roots.end(), root) == app.settings.roots.end())
        app.settings.roots.push_back(root);
    persist();
    scan();
}

void paintMain(HDC);
void paintPlayer(HDC, HWND, bool);
void layout();
void openPlayer();
void miniPlayer();
void closePlayer();
void setFullscreen(bool);
void openPicker();
void saveStack();
void toolsMenu(HWND);
void historyMenu();
void playlistMenu();
void alignStack();
void showDetails(const std::wstring &, const std::wstring &);
void loadStack(int);
} // namespace

namespace
{
void button(HDC dc, std::vector<Hit> &hits, Rect r, Action action, int data, const std::wstring &label,
            bool active = false, bool enabled = true)
{
    fill(dc, r, active ? colors.active : colors.raised);
    text(dc, translate(label, app.features.language), r, app.body,
         enabled ? (active ? colors.accent : colors.text) : colors.muted,
         DT_CENTER | DT_VCENTER | DT_SINGLELINE | DT_END_ELLIPSIS);
    if (enabled)
        hits.push_back({r, action, data});
}
void iconButton(HDC dc, std::vector<Hit> &hits, Rect r, Action action, int data, Icon icon,
                bool active = false, bool enabled = true)
{
    if (active && action != TOGGLE_STACK && action != TOGGLE_NODES)
        fill(dc, r, colors.active);
    auto color = enabled ? (active ? colors.accent : colors.muted) : colors.line;
    uiIcon(dc, icon, r, color);
    if (enabled)
        hits.push_back({r, action, data});
}
void row(HDC dc, std::vector<Hit> &hits, Rect r, const Song &song, int index, bool picker = false)
{
    if (song.path == app.tapPath)
    {
        double progress = double(GetTickCount64() - app.tapAt) / 180;
        if (progress < 1)
            r.x += int(4 * std::sin(progress * 3.141592654));
    }
    bool current = primary(song);
    if (current)
        fill(dc, r, colors.active);
    int iconSize = std::min(52, r.h - 16);
    Rect art{r.x + 12, r.y + 8, iconSize, iconSize};
    fill(dc, art, colors.raised);
    uiIcon(dc, song.video ? Icon::Video : Icon::Music, art, current ? colors.accent : colors.muted, 26);
    int actions = picker ? 48 : 100;
    int start = r.x + iconSize + 24;
    text(dc, song.title, {start, r.y + 10, r.w - (start - r.x) - actions, 24}, app.title, colors.text);
    text(dc, song.format + L" · " + bytes(song), {start, r.y + 34, r.w - (start - r.x) - actions, 18},
         app.small, colors.muted);
    hits.push_back({r, picker ? ADD_SONG : PLAY_SONG, index});
    if (current)
    {
        for (int i = 0; i < 3; ++i)
        {
            int h =
                app.playback.state == 3 ? int(5 + 10 * (std::sin(app.now / 180.0 + i * 1.9) + 1) / 2) : 10;
            fill(dc, {r.x + r.w - actions - 12 + i * 4, r.y + r.h / 2 + 7 - h, 2, h}, colors.accent);
        }
    }
    iconButton(dc, hits, {r.x + r.w - 48, r.y + (r.h - 44) / 2, 44, 44}, picker ? ADD_SONG : FAVORITE_SONG,
               index,
               picker                                    ? Icon::Plus
               : app.settings.favorites.count(song.path) ? Icon::HeartFilled
                                                         : Icon::Heart,
               !picker && app.settings.favorites.count(song.path));
    if (!picker)
        iconButton(dc, hits, {r.x + r.w - 96, r.y + (r.h - 44) / 2, 44, 44}, ADD_SONG, index, Icon::Plus,
                   false, app.slots.size() < 8);
    line(dc, r.x + 12, r.y + r.h - 1, r.x + r.w - 12, r.y + r.h - 1, colors.line);
}
void sectionHeader(HDC dc, std::vector<Hit> &hits, Rect r, const std::wstring &title, Action action, int data)
{
    fill(dc, r, colors.panel);
    text(dc, title, {r.x + 14, r.y, r.w - 64, r.h}, app.title, colors.text);
    iconButton(dc, hits, {r.x + r.w - 44, r.y, 40, r.h}, action, data, Icon::Plus);
    line(dc, r.x, r.y + r.h, r.x + r.w, r.y + r.h, colors.line);
}
void layout()
{
    if (!app.main)
        return;
    RECT client{};
    GetClientRect(app.main, &client);
    app.clientW = logical(client.right);
    app.clientH = logical(client.bottom);
    int width = app.clientW, height = app.clientH, top = 62,
        bottom = std::max(top + 80, height - dockHeight()), available = std::max(200, width - 48);
    int left = app.showStack && width >= 780
                   ? int(std::clamp(app.settings.leftWidth, 220.0, double(std::max(220, available - 400))))
                   : 0;
    int right =
        app.showNodes && width >= 1020
            ? int(std::clamp(app.settings.rightWidth, 240.0, double(std::max(240, available - left - 340))))
            : 0;
    app.stackRect = {48, top, left, bottom - top};
    app.libraryRect = {48 + left, top, available - left - right, bottom - top};
    app.nodesRect = {width - right, top, right, bottom - top};
    auto l = app.libraryRect;
    Rect edit{l.x + 42, l.y + 54, std::max(80, l.w - 100), 30};
    auto p = physical(edit);
    MoveWindow(app.search, p.left, p.top, p.right - p.left, p.bottom - p.top, TRUE);
    ShowWindow(app.search, app.settingsPage ? SW_HIDE : SW_SHOW);
    auto n = app.nodesRect;
    auto nr = physical({n.x + 38, n.y + 54, std::max(20, n.w - 94), 30});
    MoveWindow(app.nodeSearch, nr.left, nr.top, nr.right - nr.left, nr.bottom - nr.top, TRUE);
    ShowWindow(app.nodeSearch, right ? SW_SHOW : SW_HIDE);
    if (!app.player && !app.mini)
    {
        auto size = compactMetrics();
        positionPreview(app.main, {64, height - dockHeight() + (dockHeight() - int(size.y)) / 2, int(size.x),
                                   int(size.y)});
    }
    if (app.player)
    {
        RECT r{};
        GetClientRect(app.player, &r);
        int w = logical(r.right), h = logical(r.bottom);
        positionPreview(app.player, app.fullscreen
                                        ? Rect{0, 0, w, h}
                                        : Rect{0, 52, w, std::max(120, std::min(300, int(h * .39)))});
    }
    if (app.mini)
    {
        RECT r{};
        GetClientRect(app.mini, &r);
        positionPreview(app.mini, {0, 0, logical(r.right), std::max(1, logical(r.bottom) - 36)});
    }
}
void paintLibrary(HDC dc)
{
    auto r = app.libraryRect;
    int rowHeight = 64 + app.settings.row * 16;
    fill(dc, r, colors.panel);
    fill(dc, {r.x, r.y, r.w, 40}, colors.raised);
    button(dc, app.mainHits, {r.x, r.y, 104, 40}, LIBRARY_TAB, 0, L"Library", !app.settingsPage);
    button(dc, app.mainHits, {r.x + 104, r.y, 104, 40}, SETTINGS_TAB, 0, L"Settings", app.settingsPage);
    line(dc, r.x, r.y + 39, r.x + r.w, r.y + 39, colors.line);
    if (app.settingsPage)
    {
        int y = r.y + 56 - app.settingsScroll;
        int clip = SaveDC(dc);
        auto clipr = physical({r.x, r.y + 40, r.w, r.h - 40});
        IntersectClipRect(dc, clipr.left, clipr.top, clipr.right, clipr.bottom);
        const wchar_t *labels[]{L"Appearance", L"Library", L"Playback", L"About"};
        for (int i = 0; i < 4; ++i)
        {
            button(dc, app.mainHits, {r.x + 16, y, r.w - 32, 44}, SETTINGS_SECTION, i,
                   std::wstring(app.section[i] ? L"−  " : L"+  ") +
                       translate(labels[i], app.features.language));
            y += 56;
            if (!app.section[i])
                continue;
            if (i == 0)
            {
                text(dc, L"Native desktop palettes", {r.x + 24, y, r.w - 48, 24}, app.body, colors.muted);
                y += 34;
                const wchar_t *names[]{L"Luna", L"Orbit", L"Space Black"};
                for (int j = 0; j < 3; ++j)
                    button(dc, app.mainHits, {r.x + 24 + j * 104, y, 96, 38}, PALETTE, j, names[j],
                           app.settings.palette == j);
                y += 58;
            }
            if (i == 1)
            {
                text(dc, L"Row size", {r.x + 24, y, r.w - 48, 24}, app.body, colors.muted);
                y += 30;
                for (int j = 0; j < 3; ++j)
                    button(dc, app.mainHits, {r.x + 24 + j * 104, y, 96, 38}, ROW_SIZE, j,
                           j == 0   ? L"Small"
                           : j == 1 ? L"Medium"
                                    : L"Large",
                           app.settings.row == j);
                y += 56;
                text(dc, L"Media folders · removal never deletes files", {r.x + 24, y, r.w - 48, 24},
                     app.body, colors.muted);
                y += 32;
                for (std::size_t j = 0; j < app.settings.roots.size(); ++j)
                {
                    text(dc, app.settings.roots[j], {r.x + 24, y, r.w - 86, 40}, app.body, colors.text);
                    iconButton(dc, app.mainHits, {r.x + r.w - 60, y, 40, 40}, REMOVE_FOLDER, int(j),
                               Icon::Close);
                    y += 46;
                }
                button(dc, app.mainHits, {r.x + 24, y, 128, 38}, ADD_FOLDER, 0, L"Add folder");
                y += 56;
            }
            if (i == 2)
            {
                button(dc, app.mainHits, {r.x + 24, y, 200, 38}, REPEAT, 0,
                       app.settings.loop ? L"Primary / Stack loop: on" : L"Primary / Stack loop: off",
                       app.settings.loop);
                y += 56;
                text(dc, L"Mini-window size", {r.x + 24, y, r.w - 48, 24}, app.body, colors.muted);
                y += 30;
                for (int j = 0; j < 3; ++j)
                    button(dc, app.mainHits, {r.x + 24 + j * 104, y, 96, 38}, MINI_SIZE, j,
                           j == 0   ? L"Small"
                           : j == 1 ? L"Medium"
                                    : L"Large",
                           app.settings.miniSize == j);
                y += 58;
                text(dc, L"Ctrl+Space: play / pause · F11: fullscreen", {r.x + 24, y, r.w - 48, 44}, app.body,
                     colors.muted);
                y += 52;
            }
            if (i == 3)
            {
                text(dc, std::wstring(L"Greater Art Desktop ") + VERSION, {r.x + 24, y, r.w - 48, 30},
                     app.title, colors.text);
                y += 40;
                text(dc,
                     L"Original Windows-native application.\nNo VLC, Java, Electron or network "
                     L"runtime.\nBundled offline audio codecs; native Windows video.\nSound alignment is "
                     L"structural, not sample-locked.\nFull Android parity remains in progress.",
                     {r.x + 24, y, r.w - 48, 112}, app.body, colors.muted, DT_LEFT | DT_WORDBREAK);
                y += 132;
            }
        }
        RestoreDC(dc, clip);
        return;
    }
    fill(dc, {r.x + 14, r.y + 48, r.w - 28, 44}, colors.raised);
    uiIcon(dc, Icon::Search, {r.x + 18, r.y + 48, 30, 44}, colors.muted);
    iconButton(dc, app.mainHits, {r.x + r.w - 54, r.y + 48, 40, 44}, CLEAR_SEARCH, 0, Icon::Close);
    button(dc, app.mainHits, {r.x + 14, r.y + 102, 106, 34}, ALL_FILES, 0, L"All songs", !app.favoritesOnly);
    button(dc, app.mainHits, {r.x + 128, r.y + 102, 104, 34}, FAVORITES, 0, L"Favorites", app.favoritesOnly);
    iconButton(dc, app.mainHits, {r.x + r.w - 56, r.y + 100, 42, 38}, SORT, 0, Icon::Sort);
    int top = r.y + 148, rows = std::max(0, (r.y + r.h - top) / rowHeight);
    app.libraryScroll = std::clamp(app.libraryScroll, 0, std::max(0, int(app.filtered.size()) - rows));
    if (app.filtered.empty())
    {
        text(dc,
             app.busy                     ? L"Reading your library…"
             : app.settings.roots.empty() ? L"Bring your library"
                                          : L"No matching media",
             {r.x + 24, top + 40, r.w - 48, 34}, app.title, colors.text);
        text(dc,
             app.settings.roots.empty()
                 ? L"Choose a local folder to get started.\nYour files stay on your computer."
                 : L"Try another search or return to All songs.",
             {r.x + 24, top + 82, r.w - 48, 72}, app.body, colors.muted, DT_LEFT | DT_WORDBREAK);
        if (app.settings.roots.empty())
            button(dc, app.mainHits, {r.x + 24, top + 158, 136, 42}, ADD_FOLDER, 0, L"Add folder");
    }
    for (int i = 0; i < rows && app.libraryScroll + i < int(app.filtered.size()); ++i)
    {
        int index = app.filtered[app.libraryScroll + i];
        row(dc, app.mainHits, {r.x, top + i * rowHeight, r.w, rowHeight}, app.songs[index], index);
    }
    if (int(app.filtered.size()) > rows && rows > 0)
    {
        int length = std::max(24, (r.y + r.h - top) * rows / int(app.filtered.size())),
            y = top +
                (r.y + r.h - top - length) * app.libraryScroll / std::max(1, int(app.filtered.size()) - rows);
        fill(dc, {r.x + r.w - 5, y, 3, length}, colors.muted);
    }
}
void paintStack(HDC dc)
{
    auto r = app.stackRect;
    if (!r.w)
        return;
    fill(dc, r, colors.panel);
    text(dc, L"STACK", {r.x + 16, r.y + 4, r.w - 150, 40}, app.title, colors.muted);
    iconButton(dc, app.mainHits, {r.x + r.w - 132, r.y + 2, 40, 40}, LOAD_STACK, 0, Icon::Folder, false,
               !app.settings.stacks.empty());
    iconButton(dc, app.mainHits, {r.x + r.w - 88, r.y + 2, 40, 40}, SAVE_STACK, 0, Icon::Save, false,
               !app.slots.empty());
    iconButton(dc, app.mainHits, {r.x + r.w - 44, r.y + 2, 40, 40}, ADD_STACK, 0, Icon::Plus, false,
               app.slots.size() < 8);
    line(dc, r.x, r.y + 40, r.x + r.w, r.y + 40, colors.line);
    int top = r.y + 56 - app.stackScroll;
    if (app.slots.empty())
    {
        text(dc, L"One song. More voices.", {r.x + 18, r.y + 82, r.w - 36, 30}, app.title, colors.text);
        text(dc,
             L"Start a track, then add another\ntake. Up to eight sources play\ntogether with independent "
             L"gain,\nmute and solo controls.",
             {r.x + 18, r.y + 124, r.w - 36, 100}, app.body, colors.muted, DT_LEFT | DT_WORDBREAK);
        button(dc, app.mainHits, {r.x + 18, r.y + 248, r.w - 36, 42}, ADD_STACK, 0, L"Choose primary track");
    }
    int clip = SaveDC(dc);
    auto cr = physical({r.x, r.y + 44, r.w, r.h - 88});
    IntersectClipRect(dc, cr.left, cr.top, cr.right, cr.bottom);
    for (std::size_t i = 0; i < app.slots.size(); ++i)
    {
        auto &slot = app.slots[i];
        Rect card{r.x + 12, top + int(i) * 184, r.w - 24, 172};
        if (card.y + card.h < r.y + 44 || card.y >= r.y + r.h - 44)
            continue;
        fill(dc, card, i == 0 ? colors.active : colors.raised);
        text(dc, i == 0 ? L"PRIMARY" : L"VOICE " + std::to_wstring(i + 1),
             {card.x + 12, card.y + 8, card.w - 68, 22}, app.small, colors.accent);
        if (i)
            iconButton(dc, app.mainHits, {card.x + card.w - 44, card.y + 4, 40, 36}, REMOVE_SLOT, int(i),
                       Icon::Close);
        text(dc, slot.song.title, {card.x + 12, card.y + 32, card.w - 24, 26}, app.title, colors.text);
        button(dc, app.mainHits, {card.x + 12, card.y + 64, 62, 28}, MUTE_SLOT, int(i), L"Mute", slot.mute);
        button(dc, app.mainHits, {card.x + 82, card.y + 64, 62, 28}, SOLO_SLOT, int(i), L"Solo", slot.solo);
        text(dc, std::to_wstring(slot.gain) + L"%", {card.x + card.w - 58, card.y + 64, 46, 28}, app.small,
             colors.muted, DT_RIGHT | DT_VCENTER | DT_SINGLELINE);
        Rect gain{card.x + 12, card.y + 104, card.w - 24, 20};
        fill(dc, {gain.x, gain.y + 8, gain.w, 3}, colors.line);
        fill(dc, {gain.x, gain.y + 8, gain.w * slot.gain / 100, 3}, colors.accent);
        app.mainHits.push_back({gain, GAIN_SLOT, int(i)});
        if (i)
        {
            button(dc, app.mainHits, {card.x + 12, card.y + 132, 44, 28}, OFFSET_LESS, int(i), L"−.1s");
            wchar_t offset[40]{};
            swprintf_s(offset, L"%+.3f ms", slot.offset * 1000);
            Rect offsetBox{card.x + 60, card.y + 132, card.w - 124, 28};
            text(dc, offset, offsetBox, app.small, colors.accent, DT_CENTER | DT_VCENTER | DT_SINGLELINE);
            app.mainHits.push_back({offsetBox, OFFSET_OPTIONS, int(i)});
            button(dc, app.mainHits, {card.x + card.w - 56, card.y + 132, 44, 28}, OFFSET_MORE, int(i),
                   L"+.1s");
        }
    }
    RestoreDC(dc, clip);
    button(dc, app.mainHits, {r.x + 12, r.y + r.h - 40, r.w - 24, 32}, ALIGN_SOUND, 0,
           app.aligning ? L"Analysing sound…" : L"Align by sound", false,
           app.slots.size() > 1 && !app.aligning);
    line(dc, r.x + r.w - 1, r.y, r.x + r.w - 1, r.y + r.h, colors.line);
}
#include "GraphView.inl"
void transport(HDC dc, std::vector<Hit> &hits, Rect r, bool compact = false)
{
    bool playing = app.playback.state == 3 || (app.stackOutput && app.stackWanted);
    int center = r.x + r.w / 2, cy = r.y + 72;
    if (compact)
    {
        iconButton(dc, hits, {r.x + r.w / 2 - 22, r.y, 44, r.h}, PLAY_PAUSE, 0,
                   playing ? Icon::Pause : Icon::Play, true, !app.slots.empty());
        return;
    }
    text(dc, formatTime(app.playback.time), {r.x + 14, r.y + 4, 62, 28}, app.small, colors.accent);
    text(dc, app.playback.duration > 0 ? formatTime(app.playback.duration) : L"--:--",
         {r.x + r.w - 74, r.y + 4, 62, 28}, app.small, colors.muted, DT_RIGHT | DT_VCENTER | DT_SINGLELINE);
    Rect timeline{r.x + 82, r.y + 6, std::max(20, r.w - 164), 24};
    if (!app.waveform.empty())
    {
        float peak = app.wavePeak;
        if (peak > 0)
            for (int x = 0; x < timeline.w; x += 4)
            {
                int bin = int(double(x) / timeline.w * app.waveform.size());
                int height = std::clamp(
                    int(app.waveform[std::min(std::size_t(bin), app.waveform.size() - 1)] / peak * 18), 1,
                    18);
                fill(dc, {timeline.x + x, timeline.y + 12 - height / 2, 2, height}, colors.muted);
            }
    }
    fill(dc, {timeline.x, timeline.y + 11, timeline.w, 3}, colors.line);
    // Interpolate the visual thumb between clock polls, never playback commands or audio timestamps.
    double visualTime = app.playback.time;
    if (app.playback.state == 3)
        visualTime += std::min(.2, double(GetTickCount64() - app.playbackReadAt) / 1000) * app.speed;
    double fraction =
        app.playback.duration > 0 ? std::clamp(visualTime / app.playback.duration, 0.0, 1.0) : 0;
    fill(dc, {timeline.x, timeline.y + 11, int(timeline.w * fraction), 3}, colors.accent);
    if (!app.slots.empty())
    {
        circle(dc, timeline.x + int(timeline.w * fraction), timeline.y + 12, 5, colors.accent);
        hits.push_back({timeline, SEEK, 0});
    }
    iconButton(dc, hits, {center - 144, cy - 24, 48, 48}, REPEAT, 0,
               app.features.repeat == 1 ? Icon::RepeatOne : Icon::Repeat, app.settings.loop);
    if (app.practice.a >= 0)
        text(dc, app.practice.b >= 0 ? L"A–B" : L"A", {r.x + 4, r.y + 46, 50, 30}, app.small, colors.accent);
    iconButton(dc, hits, {center - 92, cy - 24, 48, 48}, PREVIOUS, 0, Icon::Previous, false,
               !app.slots.empty());
    {
        Gdiplus::Graphics graphics(dc);
        graphics.SetSmoothingMode(Gdiplus::SmoothingModeAntiAlias);
        Gdiplus::SolidBrush brush(Gdiplus::Color(255, GetRValue(colors.accent), GetGValue(colors.accent),
                                                 GetBValue(colors.accent)));
        float scale = app.dpi / 96.f;
        graphics.FillEllipse(&brush, (center - 27) * scale, (cy - 27) * scale, 54 * scale, 54 * scale);
    }
    uiIcon(dc, playing ? Icon::Pause : Icon::Play, {center - 24, cy - 24, 48, 48}, colors.bg, 30);
    if (!app.slots.empty())
        hits.push_back({{center - 28, cy - 28, 56, 56}, PLAY_PAUSE, 0});
    iconButton(dc, hits, {center + 44, cy - 24, 48, 48}, NEXT, 0, Icon::Next, false, !app.slots.empty());
    wchar_t rate[32]{};
    swprintf_s(rate, L"%.2g×", app.speed);
    button(dc, hits, {center + 98, cy - 21, 60, 42}, SPEED, 0, rate);
}
void paintMain(HDC dc)
{
    auto measuredAt = app.probeVisual ? std::chrono::steady_clock::now()
                                     : std::chrono::steady_clock::time_point{};
    auto measure = [&](int part) {
        if (app.probeVisual)
        {
            auto now = std::chrono::steady_clock::now();
            app.mainPaintParts[part] += std::chrono::duration<double, std::milli>(now - measuredAt).count();
            measuredAt = now;
        }
    };
    app.mainHits.clear();
    int w = app.clientW, h = app.clientH;
    // All visible panes paint their own background. Do not generate an Ambient
    // layer underneath them; retain an opaque base for rounding gaps on resize.
    auto base = physical({0, 0, w, h});
    SetDCBrushColor(dc, colors.bg);
    FillRect(dc, &base, static_cast<HBRUSH>(GetStockObject(DC_BRUSH)));
    fill(dc, {0, 0, 48, h}, colors.rail);
    mark(dc, {8, 12, 32, 32});
    iconButton(dc, app.mainHits, {2, 74, 44, 44}, TOGGLE_STACK, 0, Icon::Stack, app.showStack);
    iconButton(dc, app.mainHits, {2, 128, 44, 44}, LIBRARY_TAB, 0, Icon::Library, !app.settingsPage);
    iconButton(dc, app.mainHits, {2, 182, 44, 44}, TOGGLE_NODES, 0, Icon::Nodes, app.showNodes);
    iconButton(dc, app.mainHits, {2, h - 52, 44, 44}, OPEN_SETTINGS, 0, Icon::Settings, app.settingsPage);
    fill(dc, {48, 0, w - 48, 62}, colors.panel);
    text(dc, L"Greater Art", {68, 7, 280, 30}, app.heading, colors.text);
    text(dc, app.busy ? L"Reading your library…" : std::to_wstring(app.songs.size()) + L" files · offline",
         {68, 36, 380, 20}, app.small, colors.muted);
    button(dc, app.mainHits, {w - 242, 12, 132, 36}, ADD_FOLDER, 0, L"+  Add folder");
    iconButton(dc, app.mainHits, {w - 100, 8, 44, 44}, OPEN_SETTINGS, 0, Icon::Settings);
    iconButton(dc, app.mainHits, {w - 52, 8, 44, 44}, REFRESH, 0, Icon::Refresh, false, !app.busy);
    iconButton(dc, app.mainHits, {w - 294, 8, 44, 44}, HISTORY_MENU, 0, Icon::History,
               app.features.historyEnabled);
    iconButton(dc, app.mainHits, {w - 342, 8, 44, 44}, PLAYLIST_MENU, 0, Icon::Library,
               !app.activePlaylist.empty());
    iconButton(dc, app.mainHits, {w - 390, 8, 44, 44}, TOOLS, 0, Icon::More);
    measure(0);
    paintStack(dc);
    measure(1);
    paintLibrary(dc);
    measure(2);
    paintNodes(dc);
    measure(3);
    if (app.stackRect.w)
        app.mainHits.push_back({{app.libraryRect.x - 3, 62, 6, std::max(0, h - 178)}, NONE, 1});
    if (app.nodesRect.w)
        app.mainHits.push_back({{app.nodesRect.x - 3, 62, 6, std::max(0, h - 178)}, NONE, 2});
    int dock = h - dockHeight();
    fill(dc, {48, dock, w - 48, dockHeight()}, colors.rail);
    line(dc, 48, dock, w, dock, colors.line);
    int start = app.previewRect.x + app.previewRect.w + 14;
    if (app.player || app.mini)
        start = 80;
    marquee(dc, app.slots.empty() ? L"Pick a track" : app.slots[0].song.title, {start, dock + 22, 230, 30},
            app.title, colors.text);
    text(dc,
         app.now < app.statusUntil ? app.status
         : app.slots.empty()       ? L"Your files. Your player."
                                   : app.slots[0].song.format + L" · " + bytes(app.slots[0].song),
         {start, dock + 56, 230, 32}, app.small, app.now < app.statusUntil ? colors.accent : colors.muted);
    int controlsStart = w < 1020 ? 270 : std::min(w - 360, start + 250);
    transport(dc, app.mainHits,
              {controlsStart, h - 113, std::max(330, w - controlsStart - (w < 1020 ? 58 : 182)), 110});
    if (w >= 1020)
    {
        Rect volume{w - 150, dock + 48, 88, 24};
        fill(dc, {volume.x, volume.y + 11, volume.w, 3}, colors.line);
        fill(dc, {volume.x, volume.y + 11, volume.w * app.master / 100, 3}, colors.accent);
        app.mainHits.push_back({volume, MASTER_GAIN, 0});
    }
    iconButton(dc, app.mainHits, {w - 56, dock + 34, 48, 48}, EXPAND, 0, Icon::Expand, false,
               !app.slots.empty());
    measure(4);
}
void paintPlayer(HDC dc, HWND window, bool mini)
{
    RECT bounds{};
    GetClientRect(window, &bounds);
    int w = logical(bounds.right), h = logical(bounds.bottom);
    auto &hits = mini ? app.miniHits : app.playerHits;
    hits.clear();
    fill(dc, {0, 0, w, h}, colors.bg);
    if (mini)
    {
        transport(dc, hits, {0, h - 36, w, 36}, true);
        iconButton(dc, hits, {w - 36, h - 36, 36, 36}, EXPAND, 0, Icon::Expand);
        return;
    }
    if (app.fullscreen)
        return;
    iconButton(dc, hits, {8, 4, 44, 44}, SHRINK, 0, Icon::Collapse);
    iconButton(dc, hits, {58, 4, 44, 44}, HOME, 0, Icon::Home);
    iconButton(dc, hits, {w - 200, 4, 44, 44}, TOOLS, 0, Icon::More);
    iconButton(dc, hits, {w - 150, 4, 44, 44}, TRANSPORT_LOCK, 0, app.locked ? Icon::Lock : Icon::Unlock,
               app.locked);
    iconButton(dc, hits, {w - 100, 4, 44, 44}, FULLSCREEN, 0, Icon::Expand);
    iconButton(dc, hits, {w - 50, 4, 44, 44}, CLOSE_PLAYER, 0, Icon::Close);
    int identity = app.previewRect.y + app.previewRect.h;
    fill(dc, {0, identity, w, 80}, colors.panel);
    marquee(dc, app.slots.empty() ? L"Now Playing" : app.slots[0].song.title,
            {20, identity + 10, w - 154, 36}, app.title, colors.text);
    if (!app.slots.empty())
        text(dc,
             app.artist.empty() ? app.slots[0].song.format + L" · " + bytes(app.slots[0].song) : app.artist,
             {20, identity + 46, w - 154, 22}, app.small, colors.muted);
    int index = -1;
    if (!app.slots.empty())
        for (std::size_t i = 0; i < app.songs.size(); ++i)
            if (primary(app.songs[i]))
            {
                index = int(i);
                break;
            }
    iconButton(dc, hits, {w - 144, identity + 14, 44, 44}, FAVORITE_SONG, index,
               index >= 0 && app.settings.favorites.count(app.songs[index].path) ? Icon::HeartFilled
                                                                                 : Icon::Heart,
               index >= 0 && app.settings.favorites.count(app.songs[index].path), index >= 0);
    iconButton(dc, hits, {w - 94, identity + 14, 44, 44}, SHARE_FILES, index, Icon::Share, false, index >= 0);
    int top = identity + 88, rowHeight = 64, rows = std::max(0, (h - 126 - top) / rowHeight);
    for (int i = 0; i < rows && i + app.libraryScroll < int(app.filtered.size()); ++i)
    {
        int file = app.filtered[i + app.libraryScroll];
        row(dc, hits, {0, top + i * rowHeight, w, 64}, app.songs[file], file);
    }
    fill(dc, {0, h - 116, w, 116}, colors.rail);
    line(dc, 0, h - 116, w, h - 116, colors.line);
    transport(dc, hits, {12, h - 112, w - 24, 108});
}
} // namespace

#include "Tools.inl"
#include "Alignment.inl"
#include "Assets.inl"
#include "WindowsShare.inl"
#include "WindowsMedia.inl"

namespace
{
std::vector<Hit> &hitsFor(HWND window)
{
    return window == app.main     ? app.mainHits
           : window == app.mini   ? app.miniHits
           : window == app.picker ? app.pickerHits
                                  : app.playerHits;
}
Hit *hitAt(HWND window, int x, int y)
{
    auto &hits = hitsFor(window);
    for (auto i = hits.rbegin(); i != hits.rend(); ++i)
        if (i->box.contains(x, y))
            return &*i;
    return nullptr;
}
void sourceMenu(int index, HWND owner)
{
    if (index < 0 || index >= int(app.songs.size()))
        return;
    Song song = app.songs[index];
    HMENU menu = CreatePopupMenu();
    AppendMenu(menu, MF_STRING, 1, L"Play");
    AppendMenu(menu, MF_STRING, 2, L"Add to Stack");
    AppendMenu(menu, MF_STRING, 3,
               app.settings.favorites.count(song.path) ? L"Remove favorite" : L"Add favorite");
    AppendMenu(menu, MF_SEPARATOR, 0, nullptr);
    AppendMenu(menu, MF_STRING, 4, L"Copy file path");
    AppendMenu(menu, MF_STRING, 5, L"Copy Stack file paths");
    AppendMenu(menu, MF_SEPARATOR, 0, nullptr);
    AppendMenu(menu, MF_STRING, 6, L"Move file to Recycle Bin…");
    POINT cursor{};
    GetCursorPos(&cursor);
    int choice = TrackPopupMenu(menu, TPM_RETURNCMD | TPM_NONOTIFY, cursor.x, cursor.y, 0, owner, nullptr);
    DestroyMenu(menu);
    if (choice == 1)
        playSong(song);
    else if (choice == 2)
        addSong(song);
    else if (choice == 3)
        favorite(index);
    else if (choice == 4)
        clipboard(song.path);
    else if (choice == 5)
    {
        std::wstring paths;
        for (auto &slot : app.slots)
        {
            if (!paths.empty())
                paths += L"\r\n";
            paths += slot.song.path;
        }
        clipboard(paths);
    }
    else if (choice == 6)
    {
        if (MessageBox(owner, (L"Move this file to the Recycle Bin?\n\n" + song.path).c_str(),
                       L"Delete file · 1 of 3", MB_YESNO | MB_ICONWARNING | MB_DEFBUTTON2) != IDYES)
            return;
        if (MessageBox(owner, L"This changes the file on disk, not only its Library entry. Continue?",
                       L"Delete file · 2 of 3", MB_YESNO | MB_ICONWARNING | MB_DEFBUTTON2) != IDYES)
            return;
        if (MessageBox(owner, (L"Final confirmation: recycle \"" + song.title + L"\"?").c_str(),
                       L"Delete file · 3 of 3", MB_YESNO | MB_ICONWARNING | MB_DEFBUTTON2) != IDYES)
            return;
        if (std::any_of(app.slots.begin(), app.slots.end(),
                        [&](auto &slot) { return slot.song.path == song.path; }))
            releaseSlots();
        std::wstring target = song.path;
        target.push_back(0);
        SHFILEOPSTRUCT operation{};
        operation.hwnd = owner;
        operation.wFunc = FO_DELETE;
        operation.pFrom = target.c_str();
        operation.fFlags = FOF_ALLOWUNDO | FOF_NOCONFIRMATION | FOF_NOERRORUI;
        if (SHFileOperation(&operation) == 0 && !operation.fAnyOperationsAborted)
        {
            app.settings.favorites.erase(song.path);
            persist();
            scan();
            toast(L"File moved to Recycle Bin.");
        }
        else
            toast(L"The file could not be moved to Recycle Bin.");
    }
}
void loadStack(int index)
{
    if (index < 0 || index >= int(app.settings.stacks.size()))
        return;
    auto saved = app.settings.stacks[index];
    if (saved.tracks.empty())
        return;
    std::vector<Song> files;
    for (auto &track : saved.tracks)
    {
        auto found = std::find_if(app.songs.begin(), app.songs.end(),
                                  [&](auto &song) { return song.path == track.path; });
        if (found == app.songs.end())
        {
            toast(L"A saved source is missing. Existing playback was kept.");
            return;
        }
        files.push_back(*found);
    }
    playSong(files[0]);
    app.loadingStack = true;
    for (std::size_t i = 1; i < files.size(); ++i)
        addSong(files[i]);
    for (std::size_t i = 0; i < app.slots.size() && i < saved.tracks.size(); ++i)
    {
        app.slots[i].gain = saved.tracks[i].gain;
        app.slots[i].mute = saved.tracks[i].mute;
        app.slots[i].solo = saved.tracks[i].solo;
        app.slots[i].offset = saved.tracks[i].offset;
    }
    app.loadingStack = false;
    rebuildStackOutput();
    gains();
    seek(0);
    toast(L"Loaded Stack: " + saved.name);
}
LRESULT CALLBACK NameProc(HWND window, UINT message, WPARAM w, LPARAM l)
{
    if (message == WM_PAINT)
    {
        PAINTSTRUCT p{};
        auto dc = BeginPaint(window, &p);
        RECT r{};
        GetClientRect(window, &r);
        fill(dc, {0, 0, logical(r.right), logical(r.bottom)}, colors.panel);
        text(dc, L"Name this Stack", {20, 4, 384, 28}, app.title, colors.text);
        EndPaint(window, &p);
        return 0;
    }
    if (message == WM_CREATE)
    {
        auto edit =
            CreateWindowEx(0, L"EDIT", app.slots.empty() ? L"My Stack" : app.slots[0].song.title.c_str(),
                           WS_CHILD | WS_VISIBLE | WS_TABSTOP | ES_AUTOHSCROLL, px(20), px(34), px(384),
                           px(34), window, reinterpret_cast<HMENU>(301), GetModuleHandle(nullptr), nullptr);
        SendMessage(edit, WM_SETFONT, reinterpret_cast<WPARAM>(app.body), TRUE);
        SendMessage(edit, EM_SETLIMITTEXT, 128, 0);
        CreateWindow(L"BUTTON", L"Save", WS_CHILD | WS_VISIBLE | BS_DEFPUSHBUTTON, px(232), px(92), px(80),
                     px(34), window, reinterpret_cast<HMENU>(IDOK), GetModuleHandle(nullptr), nullptr);
        CreateWindow(L"BUTTON", L"Cancel", WS_CHILD | WS_VISIBLE, px(322), px(92), px(82), px(34), window,
                     reinterpret_cast<HMENU>(IDCANCEL), GetModuleHandle(nullptr), nullptr);
        SetFocus(edit);
        return 0;
    }
    if (message == WM_COMMAND && (LOWORD(w) == IDOK || LOWORD(w) == IDCANCEL))
    {
        if (LOWORD(w) == IDOK)
        {
            wchar_t name[129]{};
            GetDlgItemText(window, 301, name, 129);
            if (!*name)
                return 0;
            // Preserve the exact Stack selected when this dialog was opened, even
            // if the playing primary advances while the user types its name.
            SavedStack stack = app.pendingStackSave;
            stack.name = name;
            auto existing = std::find_if(app.settings.stacks.begin(), app.settings.stacks.end(),
                                         [&](auto &value) { return value.name == name; });
            if (existing != app.settings.stacks.end())
            {
                if (MessageBox(window, L"Replace the saved Stack with this name?", L"Save Stack",
                               MB_YESNO | MB_DEFBUTTON2) != IDYES)
                    return 0;
                *existing = stack;
            }
            else
                app.settings.stacks.push_back(stack);
            persist();
            toast(L"Saved Stack: " + stack.name);
        }
        DestroyWindow(window);
        return 0;
    }
    if (message == WM_CLOSE)
    {
        DestroyWindow(window);
        return 0;
    }
    if (message == WM_DESTROY)
    {
        EnableWindow(app.main, TRUE);
        SetForegroundWindow(app.main);
        return 0;
    }
    if (message == WM_CTLCOLOREDIT)
    {
        SetTextColor(reinterpret_cast<HDC>(w), colors.text);
        SetBkColor(reinterpret_cast<HDC>(w), colors.raised);
        return reinterpret_cast<LRESULT>(app.editBrush);
    }
    return DefWindowProc(window, message, w, l);
}
void saveStack()
{
    if (app.slots.empty())
        return;
    app.pendingStackSave = {};
    for (auto &slot : app.slots)
        app.pendingStackSave.tracks.push_back({slot.song.path, slot.gain, slot.mute, slot.solo, slot.offset});
    EnableWindow(app.main, FALSE);
    RECT r{};
    GetWindowRect(app.main, &r);
    auto window = CreateWindowEx(WS_EX_DLGMODALFRAME, L"GreaterArtName", L"Save current Stack",
                                 WS_CAPTION | WS_SYSMENU | WS_VISIBLE, r.left + px(200), r.top + px(150),
                                 px(440), px(190), app.main, nullptr, GetModuleHandle(nullptr), nullptr);
    if (!window)
        EnableWindow(app.main, TRUE);
}
void stackMenu()
{
    if (app.settings.stacks.empty())
        return;
    HMENU menu = CreatePopupMenu();
    for (std::size_t i = 0; i < app.settings.stacks.size(); ++i)
    {
        AppendMenu(menu, MF_STRING, 100 + UINT(i), app.settings.stacks[i].name.c_str());
    }
    AppendMenu(menu, MF_SEPARATOR, 0, nullptr);
    AppendMenu(menu, MF_STRING, 9001, L"Delete a saved Stack…");
    POINT cursor{};
    GetCursorPos(&cursor);
    int choice = TrackPopupMenu(menu, TPM_RETURNCMD | TPM_NONOTIFY, cursor.x, cursor.y, 0, app.main, nullptr);
    DestroyMenu(menu);
    if (choice >= 100 && choice < 100 + int(app.settings.stacks.size()))
        loadStack(choice - 100);
    else if (choice == 9001)
    {
        auto remove = CreatePopupMenu();
        for (std::size_t i = 0; i < app.settings.stacks.size(); ++i)
            AppendMenu(remove, MF_STRING, 100 + UINT(i), app.settings.stacks[i].name.c_str());
        int selected =
            TrackPopupMenu(remove, TPM_RETURNCMD | TPM_NONOTIFY, cursor.x, cursor.y, 0, app.main, nullptr);
        DestroyMenu(remove);
        if (selected >= 100 && selected < 100 + int(app.settings.stacks.size()) &&
            MessageBox(app.main, L"Delete this saved Stack? The media files are kept.", L"Delete saved Stack",
                       MB_YESNO | MB_DEFBUTTON2) == IDYES)
        {
            app.settings.stacks.erase(app.settings.stacks.begin() + selected - 100);
            persist();
            toast(L"Saved Stack deleted.");
        }
    }
}
void action(Hit hit, HWND window)
{
    if (app.locked && window == app.player && hit.action != TRANSPORT_LOCK && hit.action != FULLSCREEN &&
        hit.action != CLOSE_PLAYER && hit.action != SHRINK && hit.action != HOME)
        return;
    switch (hit.action)
    {
    case TOOLS:
        toolsMenu(window);
        break;
    case SHARE_FILES:
        shareMenu(window);
        break;
    case HISTORY_MENU:
        historyMenu();
        break;
    case PLAYLIST_MENU:
        playlistMenu();
        break;
    case TRANSPORT_LOCK:
        app.locked = !app.locked;
        invalidate();
        break;
    case ALIGN_SOUND:
        alignStack();
        break;
    case OFFSET_LESS:
    case OFFSET_MORE:
        if (hit.data > 0 && hit.data < int(app.slots.size()))
        {
            auto &slot = app.slots[hit.data];
            slot.offset = std::clamp(slot.offset + (hit.action == OFFSET_MORE ? .1 : -.1), -30.0, 30.0);
            seek(app.playback.time);
            invalidate();
        }
        break;
    case OFFSET_OPTIONS:
        if (hit.data > 0 && hit.data < int(app.slots.size()))
        {
            auto menu = CreatePopupMenu();
            const wchar_t *names[]{L"Reset offset", L"−1 ms",     L"+1 ms",    L"−0.1 ms",
                                   L"+0.1 ms",      L"−1 sample", L"+1 sample"};
            for (int i = 0; i < 7; ++i)
                AppendMenu(menu, MF_STRING | (i >= 5 && !app.playback.pcmRate ? MF_GRAYED : 0), i + 1,
                           translate(names[i], app.features.language).c_str());
            POINT position{};
            GetCursorPos(&position);
            int choice = TrackPopupMenu(menu, TPM_RETURNCMD | TPM_NONOTIFY, position.x, position.y, 0, window,
                                        nullptr);
            DestroyMenu(menu);
            if (choice)
            {
                auto &slot = app.slots[hit.data];
                double sample = app.playback.pcmRate > 0 ? 1.0 / app.playback.pcmRate : 0;
                double steps[]{0, -.001, .001, -.0001, .0001, -sample, sample};
                slot.offset = choice == 1 ? 0 : std::clamp(slot.offset + steps[choice - 1], -30.0, 30.0);
                seek(app.playback.time);
                invalidate();
            }
        }
        break;
    case ADD_FOLDER:
        addFolder();
        break;
    case OPEN_SETTINGS:
        app.settingsPage = !app.settingsPage;
        layout();
        invalidate();
        break;
    case LIBRARY_TAB:
        app.settingsPage = false;
        layout();
        invalidate();
        break;
    case SETTINGS_TAB:
        app.settingsPage = true;
        layout();
        invalidate();
        break;
    case REFRESH:
        scan();
        break;
    case TOGGLE_STACK:
        app.showStack = !app.showStack;
        layout();
        invalidate();
        break;
    case TOGGLE_NODES:
        app.showNodes = !app.showNodes;
        layout();
        invalidate();
        break;
    case ALL_FILES:
        app.activePlaylist.clear();
        app.queue.clear();
        app.favoritesOnly = false;
        filter();
        break;
    case FAVORITES:
        app.activePlaylist.clear();
        app.queue.clear();
        app.favoritesOnly = true;
        filter();
        break;
    case SORT: {
        auto menu = CreatePopupMenu();
        AppendMenu(menu, MF_STRING, 1, L"Newest first");
        AppendMenu(menu, MF_STRING, 2, L"Oldest first");
        AppendMenu(menu, MF_STRING, 3, L"Name A–Z");
        POINT p{};
        GetCursorPos(&p);
        int selected = TrackPopupMenu(menu, TPM_RETURNCMD | TPM_NONOTIFY, p.x, p.y, 0, window, nullptr);
        DestroyMenu(menu);
        if (selected)
        {
            app.settings.sort = selected - 1;
            persist();
            filter();
        }
        break;
    }
    case CLEAR_SEARCH:
        SetWindowText(app.search, L"");
        break;
    case PLAY_SONG:
        if (hit.data >= 0 && hit.data < int(app.songs.size()))
        {
            app.tapPath = app.songs[hit.data].path;
            app.tapAt = GetTickCount64();
            app.frameClock.active(app.motionEnabled);
        }
        if (hit.data >= 0 && hit.data < int(app.songs.size()))
            playSong(app.songs[hit.data]);
        break;
    case FAVORITE_SONG:
        favorite(hit.data);
        break;
    case ADD_SONG:
        if (hit.data >= 0 && hit.data < int(app.songs.size()))
        {
            addSong(app.songs[hit.data]);
            if (window == app.picker)
                DestroyWindow(app.picker);
        }
        break;
    case ADD_STACK:
        openPicker();
        break;
    case SAVE_STACK:
        saveStack();
        break;
    case LOAD_STACK:
        stackMenu();
        break;
    case MUTE_SLOT:
        if (hit.data < int(app.slots.size()))
        {
            app.slots[hit.data].mute = !app.slots[hit.data].mute;
            gains();
            invalidate();
        }
        break;
    case SOLO_SLOT:
        if (hit.data < int(app.slots.size()))
        {
            app.slots[hit.data].solo = !app.slots[hit.data].solo;
            gains();
            invalidate();
        }
        break;
    case REMOVE_SLOT:
        if (hit.data > 0 && hit.data < int(app.slots.size()))
        {
            ++app.alignmentGeneration;
            app.aligning = false;
            mediaRelease(app.slots[hit.data].player);
            app.slots.erase(app.slots.begin() + hit.data);
            rebuildStackOutput();
            gains();
            invalidate();
        }
        break;
    case PLAY_PAUSE:
        toggle();
        break;
    case PREVIOUS:
        next(-1);
        break;
    case NEXT:
        next(1);
        break;
    case REPEAT:
        app.features.repeat = (app.features.repeat + 1) % 3;
        app.settings.loop = app.features.repeat != 0;
        if (app.stackOutput)
            app.stackOutput->loop(app.settings.loop);
        persist();
        invalidate();
        break;
    case SPEED: {
        auto menu = CreatePopupMenu();
        const wchar_t *names[]{L"0.5×", L"0.75×", L"1.0×", L"1.25×", L"1.5×", L"2.0×"};
        double rates[]{.5, .75, 1, 1.25, 1.5, 2};
        for (int i = 0; i < 6; ++i)
            AppendMenu(menu, MF_STRING, i + 1, names[i]);
        POINT p{};
        GetCursorPos(&p);
        int selected = TrackPopupMenu(menu, TPM_RETURNCMD | TPM_NONOTIFY, p.x, p.y, 0, window, nullptr);
        DestroyMenu(menu);
        if (selected)
        {
            const double requested = rates[selected - 1];
            bool accepted = true;
            for (auto &slot : app.slots)
                if (slot.player)
                    accepted = SUCCEEDED(mediaRate(slot.player, requested)) && accepted;
            if (app.stackOutput)
                accepted = SUCCEEDED(app.stackOutput->rate(requested)) && accepted;
            if (accepted)
                app.speed = requested;
            else
            {
                for (auto &slot : app.slots)
                    if (slot.player)
                        mediaRate(slot.player, app.speed);
                if (app.stackOutput)
                    app.stackOutput->rate(app.speed);
                toast(L"This source does not support the chosen speed. Previous speed restored.");
            }
            invalidate();
        }
        break;
    }
    case EXPAND:
        openPlayer();
        break;
    case HOME:
        ShowWindow(app.main, SW_RESTORE);
        closePlayer();
        SetForegroundWindow(app.main);
        break;
    case SHRINK:
        miniPlayer();
        break;
    case CLOSE_PLAYER:
        closePlayer();
        break;
    case FULLSCREEN:
        setFullscreen(!app.fullscreen);
        break;
    case FIT_GRAPH:
        fitGraph();
        invalidate();
        break;
    case SCATTER_GRAPH:
        restartGraph(true);
        invalidate();
        break;
    case GRAPH_CONTROLS:
        graphControls(window);
        break;
    case PALETTE:
        app.settings.palette = hit.data;
        theme();
        persist();
        invalidate();
        break;
    case ROW_SIZE:
        app.settings.row = hit.data;
        persist();
        filter();
        break;
    case MINI_SIZE:
        app.settings.miniSize = hit.data;
        persist();
        if (app.mini)
        {
            auto size = compactMetrics();
            SetWindowPos(app.mini, nullptr, 0, 0, px(int(size.x)), px(int(size.y) + 36),
                         SWP_NOMOVE | SWP_NOZORDER);
            layout();
        }
        invalidate();
        break;
    case SETTINGS_SECTION:
        app.section[hit.data] = !app.section[hit.data];
        invalidate();
        break;
    case REMOVE_FOLDER:
        if (hit.data < int(app.settings.roots.size()))
        {
            app.settings.roots.erase(app.settings.roots.begin() + hit.data);
            persist();
            scan();
        }
        break;
    case OPEN_SOURCE_MENU:
        sourceMenu(hit.data, window);
        break;
    default:
        break;
    }
}
void applyDrag(HWND window, int x, int)
{
    auto &hits = hitsFor(window);
    auto found = std::find_if(hits.begin(), hits.end(),
                              [](auto &hit) { return hit.action == app.drag && hit.data == app.dragSlot; });
    if (found == hits.end())
        return;
    double fraction = std::clamp(double(x - found->box.x) / std::max(1, found->box.w), 0.0, 1.0);
    if (app.drag == SEEK)
        seek(fraction * app.playback.duration);
    else if (app.drag == MASTER_GAIN)
    {
        app.master = int(fraction * 100);
        gains();
    }
    else if (app.drag == GAIN_SLOT && app.dragSlot < int(app.slots.size()))
    {
        app.slots[app.dragSlot].gain = int(fraction * 100);
        gains();
    }
    invalidate();
}
} // namespace

#include "Accessibility.inl"
#include "Ambient.inl"
#include "Window.inl"
#include "Entry.inl"
