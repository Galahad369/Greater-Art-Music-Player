#pragma once
#include "GraphMotion.h"
#include <chrono>

namespace ga
{
template <class Check> void graphMotionTests(Check expect)
{
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
}
} // namespace ga
