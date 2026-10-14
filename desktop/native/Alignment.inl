namespace
{
void alignStack()
{
    if (app.slots.size() < 2 || app.aligning)
        return;
    unsigned generation = ++app.alignmentGeneration;
    app.aligning = true;
    std::vector<std::wstring> paths;
    for (auto &slot : app.slots)
        paths.push_back(slot.song.path);
    invalidate();
    app.scans.emplace_back([generation, paths] {
        SetThreadPriority(GetCurrentThread(), THREAD_PRIORITY_BELOW_NORMAL);
        auto cancelled = [generation] { return app.closing || app.alignmentGeneration != generation; };
        App::AlignmentResult result{generation, paths, {}, {}};
        auto primary = analyseAudio(paths[0], cancelled);
        if (!primary.error.empty())
            result.error = primary.error;
        for (std::size_t i = 1; i < paths.size() && !cancelled() && result.error.empty(); ++i)
        {
            auto voice = analyseAudio(paths[i], cancelled);
            if (!voice.error.empty())
            {
                result.error = voice.error;
                break;
            }
            result.matches.push_back(matchAudio(primary, voice));
        }
        if (cancelled())
            return;
        {
            std::lock_guard<std::mutex> lock(app.alignmentMutex);
            app.alignments.push_back(std::move(result));
        }
        PostMessage(app.main, ALIGN_READY, 0, 0);
    });
}
void alignmentReady()
{
    std::vector<App::AlignmentResult> results;
    {
        std::lock_guard<std::mutex> lock(app.alignmentMutex);
        results.swap(app.alignments);
    }
    for (auto &result : results)
    {
        if (result.generation != app.alignmentGeneration)
            continue;
        app.aligning = false;
        if (!result.error.empty())
        {
            toast(result.error);
            continue;
        }
        if (result.paths.size() != app.slots.size() || result.matches.size() + 1 != app.slots.size())
            continue;
        bool same = true;
        for (std::size_t i = 0; i < app.slots.size(); ++i)
            same = same && app.slots[i].song.path == result.paths[i];
        if (!same)
            continue;
        int accepted = 0;
        for (std::size_t i = 1; i < app.slots.size(); ++i)
            if (result.matches[i - 1].accepted)
            {
                app.slots[i].offset = result.matches[i - 1].offset;
                ++accepted;
            }
        seek(app.playback.time);
        toast(std::to_wstring(accepted) + L" voices aligned. Uncertain matches kept their previous offsets.");
    }
    invalidate();
}
void syncStack()
{
    if (app.barrier)
    {
        bool ready = std::all_of(app.slots.begin(), app.slots.end(), [](auto &slot) {
            int state = mediaState(slot.player).state;
            return state == 3 || state == 4 || state == 6 || state == 7;
        });
        if (ready || app.now - app.barrierAt > 2500)
        {
            app.barrier = false;
            for (auto &slot : app.slots)
                if (app.playback.time + slot.offset >= 0)
                    mediaPlay(slot.player);
        }
        return;
    }
    if (app.slots.size() < 2 || app.playback.state != 3 || app.now - app.syncAt < 500)
        return;
    app.syncAt = app.now;
    for (std::size_t i = 1; i < app.slots.size(); ++i)
    {
        auto &slot = app.slots[i];
        auto state = mediaState(slot.player);
        double desired = app.playback.time + slot.offset;
        if (desired < 0)
        {
            mediaPause(slot.player, true);
            continue;
        }
        if (state.state == 4)
        {
            mediaSeek(slot.player, desired);
            mediaPlay(slot.player);
            continue;
        }
        if (state.state != 3)
            continue;
        double drift = state.time - desired;
        if (std::abs(drift) > .5)
        {
            mediaSeek(slot.player, desired);
            mediaRate(slot.player, app.speed);
        }
        else
            mediaRate(slot.player, app.speed * correctionRate(drift));
    }
}
} // namespace
