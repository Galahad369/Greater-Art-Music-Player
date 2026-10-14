#pragma once
#include <algorithm>
#include <cmath>
#include <functional>
#include <string>
#include <vector>
namespace ga
{
struct AudioFeatures
{
    std::vector<float> energy, attacks;
    double duration = 0;
    std::wstring error;
};
AudioFeatures analyseAudio(const std::wstring &, const std::function<bool()> &, double seconds = 180);
bool audioDecodeTest(const std::wstring &);
struct AudioMatch
{
    double offset = 0, confidence = 0;
    bool accepted = false;
};
inline AudioMatch matchAudio(const AudioFeatures &primary, const AudioFeatures &voice)
{
    const int limit = 750, minimum = 200;
    if (primary.energy.size() < minimum || voice.energy.size() < minimum)
        return {};
    auto score = [&](int lag, const std::vector<float> &a, const std::vector<float> &b) {
        int start = (std::max)(0, -lag), end = (std::min)(int(a.size()), int(b.size()) - lag);
        if (end - start < minimum)
            return -1.0;
        double sa = 0, sb = 0, saa = 0, sbb = 0, sab = 0;
        int n = end - start;
        for (int i = start; i < end; ++i)
        {
            double x = a[i], y = b[i + lag];
            sa += x;
            sb += y;
            saa += x * x;
            sbb += y * y;
            sab += x * y;
        }
        double denominator = std::sqrt((std::max)(0.0, saa - sa * sa / n) * (std::max)(0.0, sbb - sb * sb / n));
        return denominator > 1e-10 ? (sab - sa * sb / n) / denominator : 0;
    };
    auto combined = [&](int lag) {
        int overlap = (std::min)(int(primary.energy.size()), int(voice.energy.size()) - lag) - (std::max)(0, -lag);
        // Short tails can correlate perfectly at the wrong lag. Confidence must
        // reward explaining the whole shared recording, not a tiny repeating hit.
        double coverage = double((std::max)(0, overlap)) / (std::min)(primary.energy.size(), voice.energy.size());
        return (.35 * score(lag, primary.energy, voice.energy) + .65 * score(lag, primary.attacks, voice.attacks)) *
               std::sqrt(coverage);
    };
    int bestLag = 0;
    double best = -1, second = -1;
    for (int lag = -limit; lag <= limit; ++lag)
    {
        double s = combined(lag);
        if (s > best)
        {
            best = s;
            bestLag = lag;
        }
    }
    for (int lag = -limit; lag <= limit; ++lag)
        if (std::abs(lag - bestLag) > 25)
            second = (std::max)(second, combined(lag));
    double left = combined(bestLag - 1), right = combined(bestLag + 1), denominator = left - 2 * best + right;
    double fine = std::abs(denominator) > 1e-8 ? std::clamp(.5 * (left - right) / denominator, -.5, .5) : 0;
    AudioMatch match;
    match.confidence = best;
    match.accepted = best > .22 && best - second > .025;
    if (match.accepted)
        match.offset = (bestLag + fine) * .02;
    return match;
}
} // namespace ga
