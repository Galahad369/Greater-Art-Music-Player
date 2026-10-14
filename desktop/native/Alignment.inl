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
    bool accepted = app.scans.emplace_back(
        [generation, paths] {
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
        },
        [generation] {
            if (!app.closing)
                PostMessage(app.main, TASK_FAILURE, 2, LPARAM(generation));
        });
    if (!accepted)
    {
        app.aligning = false;
        toast(L"Background work is busy. Try alignment again shortly.");
    }
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
    if (app.stackOutput)
    {
        auto audio = app.stackOutput->state();
        auto video = mediaState(app.slots[0].player);
        if (audio.state == 7 || (app.slots[0].song.video && video.state == 7))
        {
            app.stackOutput->pause(true);
            mediaPause(app.slots[0].player, true);
            app.stackWanted = false;
            if (app.stackPending)
                toast(L"Stack could not prepare every voice. No partial independent playback was started.");
            app.stackPending = false;
            return;
        }
        if (app.stackPending && audio.state == 4 &&
            (!app.slots[0].song.video || video.state == 3 || video.state == 4 || video.state == 6))
        {
            mediaSeek(app.slots[0].player, audio.time);
            app.stackOutput->pause(!app.stackWanted);
            mediaPause(app.slots[0].player, !app.stackWanted);
            app.stackPending = false;
            app.syncAt = app.now;
        }
        // Only the silent video presentation follows the PCM master. Audible
        // voices never seek or trim independently: they share one output block.
        if (!app.stackPending && audio.state == 3 && app.slots[0].song.video && app.now - app.syncAt >= 2000)
        {
            app.syncAt = app.now;
            if (video.state == 6 || std::abs(video.time - audio.time) > .25)
            {
                mediaSeek(app.slots[0].player, audio.time);
                mediaPlay(app.slots[0].player);
            }
        }
        return;
    }
}
} // namespace
