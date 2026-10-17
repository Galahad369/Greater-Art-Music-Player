// Uses only the application's own window, isolated settings and read-only local media.
// Sends real window messages so the test covers capture/drag/zoom and playback together.
template <class Fail> void graphProbeTick(Fail fail)
{
    static std::uint64_t at = 0, frames = 0;
    static int held = -1;
    static Point anchor{};
    static unsigned width = 0, height = 0;
    static std::vector<Point> before;
    auto now = GetTickCount64();
    auto mouse = [&](UINT message, Point p, WPARAM keys = 0) {
        SendMessage(app.main, message, keys, MAKELPARAM(px(int(p.x)), px(int(p.y))));
    };
    if (app.probeStage == 0 && !app.busy && app.playback.state == 3 && app.playback.frames > 3)
    {
        if (app.songs.size() < 3)
        {
            fail(L"graph probe requires a local library with at least three files");
            return;
        }
        width = app.playback.width;
        height = app.playback.height;
        app.motionEnabled = TRUE; // App-local test override, never a Windows preference write.
        app.settings.rightWidth = 500;
        app.probePlayer = app.slots[0].player;
        filter();
        layout();
        fitGraph();
        restartGraph(true);
        app.probeStage = 1;
        at = now;
        return;
    }
    if (app.probeStage == 1 && now - at > 2000)
    {
        fitGraph();
        for (std::size_t i = 1; i < app.graph.points.size(); ++i)
        {
            auto p = graphPoint(i);
            auto r = graphStage();
            if (graphHit(p.x, p.y) == int(i) && p.x < r.x + r.w - 60 && p.y < r.y + r.h - 40)
            {
                held = int(i);
                anchor = p;
                break;
            }
        }
        if (held < 0)
        {
            fail(L"no visible node hit target");
            return;
        }
        mouse(WM_MOUSEMOVE, anchor);
        if (app.graphHover != held)
        {
            fail(L"hover focus failed");
            return;
        }
        capture(app.main, app.probeOutput / L"nodes-hover.png");
        before = app.graph.points;
        mouse(WM_LBUTTONDOWN, anchor, MK_LBUTTON);
        // A slow drag composed entirely of <=2px deltas used to trigger Play.
        for (int i = 1; i <= 18; ++i)
            mouse(WM_MOUSEMOVE, {anchor.x + i * 2, anchor.y}, MK_LBUTTON);
        if (app.graphDrag != held || !app.graphMoved)
        {
            fail(L"slow drag not recognized");
            return;
        }
        app.probeStage = 2;
        at = now;
        return;
    }
    if (app.probeStage == 2 && now - at > 1000)
    {
        double movement = 0;
        for (auto edge : app.graph.edges)
        {
            int other = int(edge.a) == held ? int(edge.b) : int(edge.b) == held ? int(edge.a) : -1;
            if (other >= 0)
                movement += std::hypot(app.graph.points[other].x - before[other].x,
                                       app.graph.points[other].y - before[other].y);
        }
        if (movement < 1)
        {
            fail(L"neighbors do not respond to dragged spring");
            return;
        }
        auto actual = graphPoint(held);
        // SetCapture may also deliver a queued physical-cursor move. Assert
        // against the latest input actually received, never warp the user's mouse.
        Point expected{anchor.x + app.graphPointer.x - px(int(anchor.x)) * 96.0 / app.dpi,
                       anchor.y + app.graphPointer.y - px(int(anchor.y)) * 96.0 / app.dpi};
        if (std::hypot(actual.x - expected.x, actual.y - expected.y) > 2)
        {
            fail(L"dragged node escaped cursor: expected=" + std::to_wstring(expected.x) + L"," +
                 std::to_wstring(expected.y) + L" actual=" + std::to_wstring(actual.x) + L"," +
                 std::to_wstring(actual.y) + L" pin=" + std::to_wstring(app.graphDrag) + L" slot=" +
                 std::to_wstring(app.dragSlot));
            return;
        }
        mouse(WM_LBUTTONUP, {double(app.dragStartX), double(app.dragStartY)});
        if (app.slots[0].player != app.probePlayer || app.slots[0].song.path != app.probeFixture)
        {
            fail(L"drag accidentally started another song");
            return;
        }
        // Both pan gestures must work even when started directly over a node.
        // Messages execute in one UI turn so physics cannot move the camera's anchor.
        for (bool middle : {true, false})
        {
            // Capture can deliver the physical cursor outside the canvas. Bring
            // every world point back into view before starting the next gesture.
            fitGraph();
            auto start = graphPoint(held);
            if (!middle)
                SendMessage(app.main, WM_KEYDOWN, VK_SPACE, 0);
            mouse(middle ? WM_MBUTTONDOWN : WM_LBUTTONDOWN, start, middle ? MK_MBUTTON : MK_LBUTTON);
            if (app.graphDrag != -1 || GetCapture() != app.main)
            {
                auto stage = graphStage();
                fail(L"pan capture failed: middle=" + std::to_wstring(middle) + L" start=" +
                     std::to_wstring(start.x) + L"," + std::to_wstring(start.y) + L" stage=" +
                     std::to_wstring(stage.x) + L"," + std::to_wstring(stage.y) + L"," +
                     std::to_wstring(stage.w) + L"," + std::to_wstring(stage.h) + L" node=" +
                     std::to_wstring(app.graphDrag) + L" space=" + std::to_wstring(app.graphSpace));
                return;
            }
            auto pointer = app.graphPointer;
            Point destination{pointer.x + 32, pointer.y + 18};
            mouse(WM_MOUSEMOVE, destination, middle ? MK_MBUTTON : MK_LBUTTON);
            auto moved = graphPoint(held);
            if (std::hypot(moved.x - start.x - (app.graphPointer.x - pointer.x),
                           moved.y - start.y - (app.graphPointer.y - pointer.y)) > .001)
            {
                fail(L"pan does not follow pointer displacement");
                return;
            }
            mouse(middle ? WM_MBUTTONUP : WM_LBUTTONUP, destination);
            if (!middle)
                SendMessage(app.main, WM_KEYUP, VK_SPACE, 0);
            if (GetCapture() == app.main || app.graphSpace || app.graphDrag != -1 ||
                app.slots[0].player != app.probePlayer || app.slots[0].song.path != app.probeFixture)
            {
                fail(L"pan did not release cleanly or changed playback");
                return;
            }
        }
        SendMessage(app.main, WM_KEYDOWN, VK_SPACE, 0);
        SendMessage(app.main, WM_KILLFOCUS, 0, 0);
        if (app.graphSpace)
        {
            fail(L"focus loss leaves Space pan stuck");
            return;
        }
        auto p = graphPoint(held);
        zoomGraph(p.x, p.y, 1.4);
        auto zoomed = graphPoint(held);
        if (std::hypot(p.x - zoomed.x, p.y - zoomed.y) > .001)
        {
            fail(L"zoom is not anchored to cursor");
            return;
        }
        app.nodeQuery = app.songs[0].title;
        if (graphHit(zoomed.x, zoomed.y) == held)
        {
            fail(L"filtered-out node intercepts clicks");
            return;
        }
        app.nodeQuery.clear();
        app.graphHover = -1;
        fitGraph();
        capture(app.main, app.probeOutput / L"nodes-released.png");
        frames = app.playback.frames;
        app.probeStage = 3;
        at = now;
        return;
    }
    if (app.probeStage == 3 && now - at > 14000)
    {
        if (app.graphAwake)
        {
            fail(L"graph does not settle after release: simulated=" + std::to_wstring(app.graphAge) +
                 L" visualFrames=" + std::to_wstring(app.visualFrames) + L" motion=" +
                 std::to_wstring(app.motionEnabled) + L" visible=" +
                 std::to_wstring(IsWindowVisible(app.main)) + L" iconic=" +
                 std::to_wstring(IsIconic(app.main)) + L" drag=" + std::to_wstring(app.graphDrag));
            return;
        }
        if (app.slots[0].player != app.probePlayer || app.playback.frames <= frames ||
            app.playback.width != width || app.playback.height != height)
        {
            fail(L"native video did not continue unchanged during graph interaction");
            return;
        }
        fitGraph();
        capture(app.main, app.probeOutput / L"nodes-settled.png");
        app.motionEnabled = FALSE;
        restartGraph(true);
        if (app.graphRevealAt)
        {
            fail(L"reduced motion still reveals nodes sequentially");
            return;
        }
        app.probeStage = 4;
        at = now;
        return;
    }
    if (app.probeStage == 4 && now - at > 2000)
    {
        if (app.graphAwake)
        {
            fail(L"reduced-motion final layout did not settle");
            return;
        }
        std::ofstream report(app.probeOutput / L"verification.txt");
        report
            << "PASS graph hover + slow drag + connected-node springs + cursor zoom + filtered hit testing\n"
            << "PASS middle-mouse and Space+drag pan over nodes, clean release and focus-loss reset\n"
            << "PASS idle settling and reduced-motion final layout\n"
            << "PASS same native player, source=" << app.playback.width << "x" << app.playback.height
            << " frames=" << app.playback.frames << "\n";
        SystemParametersInfo(SPI_GETCLIENTAREAANIMATION, 0, &app.motionEnabled, 0);
        PostMessage(app.main, WM_CLOSE, 0, 0);
        app.probeStage = 5;
    }
}
