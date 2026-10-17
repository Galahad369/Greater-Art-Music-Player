Rect graphStage()
{
    auto r = app.nodesRect;
    return {r.x + 1, r.y + 102, std::max(0, r.w - 2), std::max(0, r.h - 142)};
}
double graphScale()
{
    auto r = graphStage();
    return std::max(.001, std::min((r.w - 40) / std::max(80.0, app.graphMaxX - app.graphMinX),
                                   (r.h - 40) / std::max(80.0, app.graphMaxY - app.graphMinY)) *
                              app.zoom);
}
Point graphPoint(std::size_t i)
{
    if (i >= app.graph.points.size())
        return {};
    auto r = graphStage();
    auto p = app.graph.points[i];
    double scale = graphScale();
    return {r.x + r.w / 2.0 + (p.x - (app.graphMinX + app.graphMaxX) / 2) * scale + app.panX,
            r.y + r.h / 2.0 + (p.y - (app.graphMinY + app.graphMaxY) / 2) * scale + app.panY};
}
void fitGraph()
{
    app.graphMinX = app.graphMinY = -40;
    app.graphMaxX = app.graphMaxY = 40;
    for (auto p : app.graph.points)
    {
        app.graphMinX = std::min(app.graphMinX, p.x - 18);
        app.graphMaxX = std::max(app.graphMaxX, p.x + 18);
        app.graphMinY = std::min(app.graphMinY, p.y - 18);
        app.graphMaxY = std::max(app.graphMaxY, p.y + 18);
    }
    app.zoom = 1;
    app.panX = app.panY = 0;
}
void restartGraph(bool reveal = false)
{
    app.graphHover = app.graphDrag = -1;
    if (reveal && app.motionEnabled)
        for (auto &p : app.graph.points)
        {
            p.x *= .82;
            p.y *= .82;
        }
    app.graphRevealAt = reveal && app.motionEnabled ? GetTickCount64() : 0;
    app.graphEpoch = app.graphMotion.reset(app.graph,
                                           {app.features.graphCenter, app.features.graphRepel,
                                            app.features.graphLink, app.features.graphDistance},
                                           !app.motionEnabled);
    app.graphAwake = app.motionEnabled && app.graph.points.size() > 1;
    app.graphFrameAt = GetTickCount64();
    app.frameClock.active(app.graphAwake || app.graphRevealAt);
}
double graphAppearance(std::size_t i)
{
    if (!app.graphRevealAt || !app.motionEnabled)
        return 1;
    double delay = 500.0 * i / std::max(std::size_t(1), app.graph.points.size());
    double t = std::clamp((double(GetTickCount64() - app.graphRevealAt) - delay) / 420.0, 0.0, 1.0);
    // Small spring overshoot, followed by a stable circle; never a perpetual pulse.
    double u = t - 1;
    return 1 + 2.1 * u * u * u + 1.1 * u * u;
}
bool graphVisible(std::size_t i)
{
    return i < app.graph.points.size() && i < app.songs.size() && graphAppearance(i) > .05 &&
           (app.nodeQuery.empty() ||
            lower(app.songs[i].title).find(lower(app.nodeQuery)) != std::wstring::npos);
}
double graphRadius(std::size_t i)
{
    double weight = i < app.graph.weights.size() ? app.graph.weights[i] : 0;
    return std::clamp((6 + 12 * std::pow(std::clamp(weight, 0.0, 1.0), .72)) * graphScale(), 2.0, 28.0);
}
int graphHit(double x, double y)
{
    if (!graphStage().contains(int(x), int(y)))
        return -1;
    int best = -1;
    double closest = 1e9;
    for (std::size_t i = 0; i < app.graph.points.size(); ++i)
        if (graphVisible(i))
        {
            auto p = graphPoint(i);
            double distance = std::hypot(p.x - x, p.y - y);
            if (distance <= std::max(8.0, graphRadius(i) + 3) && distance < closest)
            {
                best = int(i);
                closest = distance;
            }
        }
    return best;
}
void invalidateGraph()
{
    auto r = physical(graphStage());
    InvalidateRect(app.main, &r, FALSE);
}
void tickGraph()
{
    if (!app.showNodes || app.settingsPage || !app.nodesRect.w || !IsWindowVisible(app.main) ||
        IsIconic(app.main))
    {
        app.graphFrameAt = GetTickCount64();
        return;
    }
    auto now = GetTickCount64();
    if (auto frame = app.graphMotion.take();
        frame && frame->generation == app.graphEpoch && frame->points.size() == app.graph.points.size())
    {
        app.graph.points = frame->points;
        app.graphAwake = frame->awake;
        app.graphAge = frame->age;
        if (app.dragSlot == 200 && app.graphDrag >= 0)
            app.graph.points[app.graphDrag] = app.graphPinned;
        invalidateGraph();
    }
    if (!app.motionEnabled)
    {
        app.graphFrameAt = now;
        return;
    }
    if (app.graphAwake || (app.dragSlot == 200 && app.graphDrag >= 0))
    {
        if (app.graphMotion.request(std::min(.25, (now - app.graphFrameAt) / 1000.0),
                                    app.dragSlot == 200 ? app.graphDrag : -1, app.graphPinned))
            app.graphFrameAt = now;
    }
    else
        app.graphFrameAt = now;
    if (app.graphRevealAt)
    {
        invalidateGraph();
        if (now - app.graphRevealAt >= 920)
            app.graphRevealAt = 0;
    }
}
void zoomGraph(double x, double y, double factor)
{
    auto r = graphStage();
    double next = std::clamp(app.zoom * factor, .25, 8.0), ratio = next / app.zoom;
    app.panX = (x - r.x - r.w / 2.0) * (1 - ratio) + app.panX * ratio;
    app.panY = (y - r.y - r.h / 2.0) * (1 - ratio) + app.panY * ratio;
    app.zoom = next;
}
void graphControls(HWND owner)
{
    auto menu = CreatePopupMenu();
    auto add = [&](HMENU m, UINT flags, UINT_PTR id, const std::wstring &label) {
        auto translated = translate(label, app.features.language);
        AppendMenu(m, flags, id, translated.c_str());
    };
    int current[]{app.features.graphCenter, app.features.graphRepel, app.features.graphLink,
                  app.features.graphDistance};
    const wchar_t *names[]{L"Center force", L"Repel force", L"Link force", L"Link distance"};
    constexpr int choices[]{25, 50, 100, 150, 200};
    for (int group = 0; group < 4; ++group)
    {
        auto sub = CreatePopupMenu();
        for (int j = 0; j < 5; ++j)
            add(sub, MF_STRING | (current[group] == choices[j] ? MF_CHECKED : 0), 100 + group * 10 + j,
                std::to_wstring(choices[j]) + L"%");
        add(menu, MF_POPUP, reinterpret_cast<UINT_PTR>(sub), names[group]);
    }
    add(menu, MF_SEPARATOR, 0, L"");
    add(menu, MF_STRING, 1, L"Restore graph defaults");
    POINT point{};
    GetCursorPos(&point);
    int choice = TrackPopupMenu(menu, TPM_RETURNCMD | TPM_NONOTIFY, point.x, point.y, 0, owner, nullptr);
    DestroyMenu(menu);
    if (!choice)
        return;
    int *values[]{&app.features.graphCenter, &app.features.graphRepel, &app.features.graphLink,
                  &app.features.graphDistance};
    if (choice == 1)
        for (auto v : values)
            *v = 100;
    else if (choice >= 100 && choice <= 134 && (choice - 100) % 10 < 5)
        *values[(choice - 100) / 10] = choices[(choice - 100) % 10];
    restartGraph();
    persist();
    invalidateGraph();
}
void paintNodes(HDC dc)
{
    auto r = app.nodesRect;
    if (!r.w)
        return;
    fill(dc, r, colors.panel);
    line(dc, r.x, r.y, r.x, r.y + r.h, colors.line);
    text(dc, translate(L"NODES", app.features.language), {r.x + 16, r.y + 4, r.w - 144, 40}, app.title,
         colors.muted);
    iconButton(dc, app.mainHits, {r.x + r.w - 126, r.y + 2, 40, 40}, GRAPH_CONTROLS, 0, Icon::Settings);
    iconButton(dc, app.mainHits, {r.x + r.w - 84, r.y + 2, 40, 40}, SCATTER_GRAPH, 0, Icon::Magic);
    iconButton(dc, app.mainHits, {r.x + r.w - 42, r.y + 2, 40, 40}, FIT_GRAPH, 0, Icon::Fit);
    line(dc, r.x, r.y + 40, r.x + r.w, r.y + 40, colors.line);
    fill(dc, {r.x + 12, r.y + 48, r.w - 24, 44}, colors.raised);
    uiIcon(dc, Icon::Search, {r.x + 14, r.y + 48, 30, 44}, colors.muted);
    text(dc, translate(L"Pan: middle mouse / Space + drag · Wheel: zoom", app.features.language),
         {r.x + 14, r.y + r.h - 36, r.w - 28, 28}, app.small, colors.muted);
    auto stage = graphStage();
    auto clip = physical(stage);
    RECT dirty{}, intersection{};
    GetClipBox(dc, &dirty);
    if (!IntersectRect(&intersection, &dirty, &clip))
        return;
    int saved = SaveDC(dc);
    IntersectClipRect(dc, clip.left, clip.top, clip.right, clip.bottom);
    auto n = app.graph.points.size();
    std::vector<Point> positions(n);
    std::vector<bool> visible(n), connected(n);
    int focus = app.dragSlot == 200 && app.graphDrag >= 0 ? app.graphDrag : app.graphHover;
    for (auto e : app.graph.edges)
        if (e.a < n && e.b < n && (int(e.a) == focus || int(e.b) == focus))
            connected[e.a] = connected[e.b] = true;
    for (std::size_t i = 0; i < n; ++i)
    {
        positions[i] = graphPoint(i);
        visible[i] = graphVisible(i);
    }
    float dpi = app.dpi / 96.0f;
    auto color = [](COLORREF value, BYTE alpha = 255) {
        return Gdiplus::Color(alpha, GetRValue(value), GetGValue(value), GetBValue(value));
    };
    {
        Gdiplus::Graphics g(dc);
        g.SetClip(Gdiplus::Rect(clip.left, clip.top, clip.right - clip.left, clip.bottom - clip.top));
        g.SetSmoothingMode(Gdiplus::SmoothingModeAntiAlias);
        g.SetPixelOffsetMode(Gdiplus::PixelOffsetModeHalf);
        // Batch equal-style geometry. Per-edge GDI+ draws dominated a large
        // library's frame time even though physics itself was off-thread.
        Gdiplus::GraphicsPath edgePaths[6];
        for (auto e : app.graph.edges)
            if (e.a < n && e.b < n && visible[e.a] && visible[e.b])
            {
                bool highlighted = int(e.a) == focus || int(e.b) == focus;
                int style = highlighted ? 5 : focus >= 0 ? 4 : std::min(3, int(e.strength * 4));
                edgePaths[style].StartFigure();
                edgePaths[style].AddLine(float(positions[e.a].x) * dpi, float(positions[e.a].y) * dpi,
                                         float(positions[e.b].x) * dpi, float(positions[e.b].y) * dpi);
            }
        for (int style = 0; style < 6; ++style)
        {
            BYTE alpha = BYTE(style == 5 ? 210 : style == 4 ? 22 : 55 + 24 * style);
            Gdiplus::Pen pen(color(style == 5 ? colors.accent : colors.muted, alpha),
                             float(style == 5   ? 1.5
                                   : style == 4 ? .8
                                                : .7 + .25 * style) *
                                 dpi);
            g.DrawPath(&pen, &edgePaths[style]);
        }
        Gdiplus::GraphicsPath nodePaths[3], rings;
        for (auto &path : nodePaths)
            path.SetFillMode(Gdiplus::FillModeWinding);
        for (std::size_t i = 0; i < n; ++i)
            if (visible[i])
            {
                bool active = primary(app.songs[i]);
                bool highlighted = int(i) == focus || connected[i];
                float radius = float(graphRadius(i) * graphAppearance(i)) * dpi;
                float x = float(positions[i].x) * dpi, y = float(positions[i].y) * dpi;
                int style = active || highlighted ? 2 : focus >= 0 ? 0 : 1;
                nodePaths[style].AddEllipse(x - radius, y - radius, radius * 2, radius * 2);
                if (active || int(i) == focus)
                {
                    float ring = radius + 3.5f * dpi;
                    rings.AddEllipse(x - ring, y - ring, ring * 2, ring * 2);
                }
            }
        for (int style = 0; style < 3; ++style)
        {
            Gdiplus::SolidBrush brush(
                color(style == 2 ? colors.accent : colors.muted, style == 0 ? 65 : 255));
            g.FillPath(&brush, &nodePaths[style]);
        }
        Gdiplus::Pen pen(color(colors.accent, 170), 1.2f * dpi);
        g.DrawPath(&pen, &rings);
    }
    // Labels appear with focus/zoom, with simple collision rejection at low zoom.
    std::vector<Rect> labels;
    for (std::size_t i = 0; i < n; ++i)
        if (visible[i] &&
            (int(i) == focus || primary(app.songs[i]) ||
             (focus < 0 && (app.zoom > 1.8 || (i < app.graph.weights.size() && app.graph.weights[i] > .7)))))
        {
            auto p = positions[i];
            int width = std::min(190, stage.w - 12);
            Rect label{std::clamp(int(p.x + graphRadius(i) + 7), stage.x + 6, stage.x + stage.w - width - 6),
                       int(p.y - 12), width, 24};
            if (label.y < stage.y || label.y + label.h > stage.y + stage.h)
                continue;
            bool overlap = false;
            for (auto old : labels)
                if (label.x < old.x + old.w && label.x + label.w > old.x && label.y < old.y + old.h &&
                    label.y + label.h > old.y)
                    overlap = true;
            if (overlap && int(i) != focus)
                continue;
            if (int(i) == focus)
                fill(dc, label, colors.panel);
            text(dc, app.songs[i].title, label, app.small, int(i) == focus ? colors.text : colors.muted);
            labels.push_back(label);
        }
    RestoreDC(dc, saved);
}
