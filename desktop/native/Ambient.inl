namespace
{
void updateAmbient()
{
    void *primary = app.slots.empty() ? nullptr : app.slots[0].player;
    bool visible = (app.main && IsWindowVisible(app.main) && !IsIconic(app.main)) ||
                   (app.player && IsWindowVisible(app.player) && !IsIconic(app.player) && !app.fullscreen);
    bool enabled =
        primary && ambientSamplingAllowed(visible, app.fullscreen, app.features.highContrast,
                                          app.playback.ambientUnavailable, app.features.background);
    if (primary != app.ambientPlayer || enabled != app.ambientEnabled)
    {
        if (app.ambientPlayer && app.ambientPlayer != primary)
            mediaAmbientEnable(app.ambientPlayer, false);
        mediaAmbientEnable(primary, enabled);
        app.ambientPlayer = primary;
        app.ambientEnabled = enabled;
    }
    auto palette = app.playback.palette;
    if (app.playback.ambientUnavailable &&
        (app.ambientTarget[0] != app.artworkColors[0] || app.ambientTarget[1] != app.artworkColors[1]))
    {
        app.ambientAt = app.now;
        for (int i = 0; i < 2; ++i)
        {
            app.ambientFrom[i] = app.ambient[i];
            app.ambientTarget[i] = app.artworkColors[i];
        }
    }
    if (palette.generation != app.ambientGeneration)
    {
        app.ambientGeneration = palette.generation;
        app.ambientSamples = 0;
    }
    if (enabled && palette.samples && palette.samples != app.ambientSamples)
    {
        app.ambientSamples = palette.samples;
        bool changed = false;
        for (int i = 0; i < 2; ++i)
            changed = changed || app.ambientTarget[i] != palette.colors[i];
        if (changed)
            app.ambientAt = app.now;
        for (int i = 0; i < 2; ++i)
        {
            if (changed)
                app.ambientFrom[i] = app.ambient[i];
            app.ambientTarget[i] = palette.colors[i];
        }
        app.ambientBrightness = palette.brightness;
        app.ambientSaturation = palette.saturation;
    }
    double mix = std::clamp(double(app.now - app.ambientAt) / 1600, 0.0, 1.0);
    mix = mix * mix * (3 - 2 * mix);
    for (int i = 0; i < 2; ++i)
    {
        auto a = app.ambientFrom[i], b = app.ambientTarget[i];
        app.ambient[i] = RGB(int(GetRValue(a) + (GetRValue(b) - GetRValue(a)) * mix),
                             int(GetGValue(a) + (GetGValue(b) - GetGValue(a)) * mix),
                             int(GetBValue(a) + (GetBValue(b) - GetBValue(a)) * mix));
    }
}
} // namespace
