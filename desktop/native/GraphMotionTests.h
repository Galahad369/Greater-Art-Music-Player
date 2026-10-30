#pragma once
#include "GraphMotion.h"
#include <chrono>

namespace ga
{
template <class Check> void graphMotionTests(Check expect)
{
    for (std::size_t count : {std::size_t(1295), std::size_t(1258)})
    {
        std::vector<Song> songs(count);
        for (std::size_t i = 0; i < count; ++i)
            songs[i].path = L"C:\\GraphFixture\\" + std::to_wstring(i) + L"\\shared stem.mp3";
        const Graph graph = buildGraph(songs, [] { return false; });
        std::set<std::pair<std::size_t, std::size_t>> edges;
        std::vector<double> incident(count);
        std::vector<std::size_t> neighbors(count);
        bool valid = graph.points.size() == count && graph.weights.size() == count;
        for (auto edge : graph.edges)
        {
            valid &= edge.a < edge.b && edge.b < count && edge.strength == 1 &&
                     edges.emplace(edge.a, edge.b).second;
            if (edge.a < count && edge.b < count)
            {
                ++neighbors[edge.a];
                ++neighbors[edge.b];
                incident[edge.a] += edge.strength;
                incident[edge.b] += edge.strength;
            }
        }
        double maximum = *std::max_element(incident.begin(), incident.end());
        for (std::size_t i = 0; i < count && valid; ++i)
        {
            const Point point = graph.points[i];
            double weight = graph.weights[i];
            valid &= neighbors[i] >= 6 && std::isfinite(point.x) && std::isfinite(point.y) &&
                     std::abs(weight - incident[i] / maximum) < 1e-12;
        }
        expect(valid, count == 1295 ? "1295-song sampling retains six unique neighbors and accurate hub weights"
                                    : "1258-song sampling retains six unique neighbors and accurate hub weights");
    }
    auto pair = [](double strength) { return Graph{{{-100, 0}, {100, 0}}, {{0, 1, strength}}, {1, 1}}; };
    auto run = [](GraphSimulation &sim, double seconds, int rate) {
        for (int i = 0; i < int(seconds * rate); ++i)
            sim.advance(1.0 / rate);
    };
    GraphSimulation strong, weak;
    strong.reset(pair(.95));
    weak.reset(pair(.15));
    run(strong, 8, 60);
    run(weak, 8, 60);
    double nearDistance = strong.graph.points[1].x - strong.graph.points[0].x;
    double farDistance = weak.graph.points[1].x - weak.graph.points[0].x;
    expect(nearDistance >= 36 && farDistance > nearDistance * 1.5,
           "graph strong links settle closer without overlapping");
    GraphSimulation fast, slow;
    fast.reset(pair(.7));
    slow.reset(pair(.7));
    run(fast, 2, 180);
    run(slow, 2, 60);
    expect(std::abs(fast.graph.points[0].x - slow.graph.points[0].x) < 1e-7,
           "graph dynamics match at 60 and 180 display updates");
    Point held{160, 70};
    auto before = strong.graph.points[0];
    for (int i = 0; i < 180; ++i)
        strong.advance(1.0 / 180, 1, held);
    expect(strong.graph.points[1].x == held.x && strong.graph.points[1].y == held.y &&
               strong.graph.points[0].x > before.x + 10,
           "graph pin stays under cursor and connected neighbor follows");
    run(strong, 14, 60);
    expect(!strong.awake, "graph sleeps after release instead of perpetual jitter");
    strong.advance(1.0 / 60, 1, held);
    expect(strong.awake, "graph drag reheats settled physics");
    Graph coincident;
    coincident.points.resize(32);
    coincident.weights.resize(32);
    GraphSimulation overlap;
    overlap.reset(coincident);
    run(overlap, 1, 60);
    bool separated = true;
    for (auto p : overlap.graph.points)
        separated &= std::isfinite(p.x) && std::isfinite(p.y);
    expect(separated && std::hypot(overlap.graph.points[0].x - overlap.graph.points[1].x,
                                   overlap.graph.points[0].y - overlap.graph.points[1].y) > 1,
           "coincident graph nodes separate with finite forces");
    Graph large;
    for (int i = 0; i < 2000; ++i)
    {
        large.points.push_back(
            {std::cos(i * 2.4) * std::sqrt(i + 1.0) * 12, std::sin(i * 2.4) * std::sqrt(i + 1.0) * 12});
        large.weights.push_back(.2);
        if (i)
            large.edges.push_back({std::size_t(i - 1), std::size_t(i), .3});
    }
    GraphSimulation scale;
    scale.reset(large);
    auto started = std::chrono::steady_clock::now();
    for (int i = 0; i < 12; ++i)
        scale.advance(1.0 / 180);
    auto spent = std::chrono::duration<double>(std::chrono::steady_clock::now() - started).count();
    bool finite = scale.graph.points.size() == 2000;
    for (auto p : scale.graph.points)
        finite &= std::isfinite(p.x) && std::isfinite(p.y);
    expect(finite && spent < 5, "2000-node spatial tree retains every node with bounded work");
    GraphSimulation empty, single;
    empty.reset({});
    empty.advance(.1);
    single.reset(Graph{{{0, 0}}, {}, {0}});
    single.advance(.1);
    expect(!empty.awake && !single.awake && single.graph.points[0].x == 0,
           "empty and single-node graphs stay stable");
    GraphMotion worker;
    worker.start();
    worker.reset(large, {});
    auto epoch = worker.reset(pair(.8), {});
    worker.request(.02, -1, {});
    std::shared_ptr<const GraphFrame> result;
    for (int i = 0; i < 200; ++i)
    {
        auto next = worker.take();
        if (next && next->generation == epoch)
        {
            result = next;
            break;
        }
        Sleep(5);
    }
    expect(result && result->points.size() == 2, "graph worker discards results from an obsolete library");
    worker.stop();

    auto frame = [](GraphMotion &motion) {
        for (int i = 0; i < 1000; ++i)
        {
            if (auto next = motion.take())
                return next;
            Sleep(2);
        }
        return std::shared_ptr<const GraphFrame>{};
    };
    auto submit = [](GraphMotion &motion, int pin, Point point) {
        for (int i = 0; i < 1000; ++i)
        {
            if (motion.request(0, pin, point))
                return;
            Sleep(2);
        }
    };
    GraphMotion drops;
    drops.reset(pair(.8), {}, true);
    drops.request(0, 0, {-210, 75});
    drops.release(0, {-245, 86});
    drops.release(1, {222, -64});
    drops.release(0, {-280, 103});
    drops.start();
    auto dropped = frame(drops);
    expect(dropped && dropped->points[0].x == -280 && dropped->points[0].y == 103 &&
               dropped->points[1].x == 222 && dropped->points[1].y == -64,
           "coalesced releases commit the latest drop for every node before unpinning");
    submit(drops, -1, {});
    auto unpinned = frame(drops);
    expect(unpinned && unpinned->points[0].x == -280 && unpinned->points[1].x == 222,
           "an old coalesced pin cannot overwrite the final drop");
    submit(drops, 0, {-310, 120});
    auto repinned = frame(drops);
    expect(repinned && repinned->points[0].x == -310 && repinned->points[0].y == 120,
           "a newer drag can pin a previously released node");
    drops.reset(large, {}, true);
    Sleep(2);
    drops.release(0, {8765, -4321});
    auto interrupted = frame(drops);
    expect(interrupted && interrupted->points.size() == large.points.size() &&
               interrupted->points[0].x == 8765 && interrupted->points[0].y == -4321,
           "release interrupts settling and suppresses an older in-flight frame");
    drops.stop();

    GraphMotion canceled;
    canceled.reset(pair(.8), {});
    canceled.release(0, {9999, 9999});
    auto newEpoch = canceled.reset(pair(.8), {});
    canceled.start();
    auto fresh = frame(canceled);
    expect(fresh && fresh->generation == newEpoch && fresh->points[0].x == -100 &&
               fresh->points[0].y == 0,
           "library reset cancels pending drops from the previous generation");
    expect(!canceled.release(-1, {}) && !canceled.release(2, {}) &&
               !canceled.release(0, {INFINITY, 0}),
           "release validates the node and final position");
    canceled.stop();
}
} // namespace ga
