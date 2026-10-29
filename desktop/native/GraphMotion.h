#pragma once
#include "Core.h"
#include <array>
#include <atomic>
#include <memory>
#include <mutex>
#include <numeric>
#include <thread>

namespace ga
{
struct GraphForces
{
    int center = 100, repel = 100, link = 100, distance = 100;
};

// World-space physics; independent of zoom, monitor refresh and the audio clock.
// Barnes-Hut repulsion bounds large-library work without dropping any nodes.
class GraphSimulation
{
    struct Cell
    {
        double x, y, size;
        Point mass{};
        std::size_t begin, end;
        int child[4]{-1, -1, -1, -1};
    };
    std::vector<Cell> tree;
    std::vector<std::size_t> order, scratch;
    std::vector<Point> velocity, force;
    std::vector<double> degree;
    double elapsed = 0, accumulator = 0, quiet = 0;
    int lastPin = -1;

    int cell(std::size_t begin, std::size_t end, double x, double y, double size, int depth)
    {
        int index = int(tree.size());
        tree.push_back({x, y, size, {}, begin, end});
        Point mass{};
        for (auto k = begin; k < end; ++k)
        {
            mass.x += graph.points[order[k]].x;
            mass.y += graph.points[order[k]].y;
        }
        mass.x /= double(end - begin);
        mass.y /= double(end - begin);
        tree[index].mass = mass;
        if (end - begin <= 8 || depth >= 20)
            return index;
        auto quadrant = [&](std::size_t i) {
            return (graph.points[i].x >= x + size / 2 ? 1 : 0) | (graph.points[i].y >= y + size / 2 ? 2 : 0);
        };
        std::size_t counts[4]{}, starts[5]{begin};
        for (auto k = begin; k < end; ++k)
            ++counts[quadrant(order[k])];
        for (int q = 0; q < 4; ++q)
            starts[q + 1] = starts[q] + counts[q];
        auto cursor = std::array<std::size_t, 4>{starts[0], starts[1], starts[2], starts[3]};
        for (auto k = begin; k < end; ++k)
            scratch[cursor[quadrant(order[k])]++] = order[k];
        std::copy(scratch.begin() + begin, scratch.begin() + end, order.begin() + begin);
        for (int q = 0; q < 4; ++q)
            if (counts[q])
            {
                int child = cell(starts[q], starts[q + 1], x + (q & 1) * size / 2, y + (q >> 1) * size / 2,
                                 size / 2, depth + 1);
                tree[index].child[q] = child;
            }
        return index;
    }
    double radius(std::size_t i) const
    {
        double weight = i < graph.weights.size() ? graph.weights[i] : 0;
        return 6 + 12 * std::pow(std::clamp(weight, 0.0, 1.0), .72);
    }
    void repel(std::size_t i, Point other, double count, double otherRadius = 0)
    {
        Point d{graph.points[i].x - other.x, graph.points[i].y - other.y};
        double square = d.x * d.x + d.y * d.y;
        // Coincident starts must separate deterministically, without a random
        // per-frame shake or a division by zero.
        if (square < .001)
        {
            d = {std::cos(i * 2.3999632297), std::sin(i * 2.3999632297)};
            square = 1;
        }
        double amount = 46 * options.repel / 100.0 * count / (square + 36);
        if (otherRadius > 0)
        {
            double distance = std::sqrt(square), clearance = radius(i) + otherRadius + 4;
            if (distance < clearance)
                amount += .32 * (clearance - distance) / distance;
        }
        force[i].x += d.x * amount;
        force[i].y += d.y * amount;
    }
    void visit(std::size_t i, int index)
    {
        const auto &c = tree[index];
        auto p = graph.points[i];
        double dx = p.x - c.mass.x, dy = p.y - c.mass.y;
        bool contains = p.x >= c.x && p.x <= c.x + c.size && p.y >= c.y && p.y <= c.y + c.size;
        double clearance = c.size * 1.414 + radius(i) + 22;
        if (!contains && c.size * c.size < .49 * (dx * dx + dy * dy) &&
            dx * dx + dy * dy > clearance * clearance)
        {
            repel(i, c.mass, double(c.end - c.begin));
            return;
        }
        bool leaf = true;
        for (int child : c.child)
            if (child >= 0)
            {
                leaf = false;
                visit(i, child);
            }
        if (leaf)
            for (auto k = c.begin; k < c.end; ++k)
                if (order[k] != i)
                    repel(i, graph.points[order[k]], 1, radius(order[k]));
    }

  public:
    Graph graph;
    GraphForces options;
    bool awake = false;
    double age() const
    {
        return elapsed;
    }
    void reset(Graph value, GraphForces settings = {})
    {
        graph = std::move(value);
        options = settings;
        auto n = graph.points.size();
        velocity.assign(n, {});
        force.resize(n);
        order.resize(n);
        scratch.resize(n);
        degree.assign(n, 1);
        for (auto e : graph.edges)
            if (e.a < n && e.b < n)
            {
                degree[e.a] += e.strength;
                degree[e.b] += e.strength;
            }
        elapsed = accumulator = quiet = 0;
        lastPin = -1;
        awake = n > 1;
    }
    void advance(double seconds, int pin = -1, Point pinned = {})
    {
        if (!std::isfinite(seconds) || seconds < 0)
            return;
        if (pin >= int(graph.points.size()) || pin < -1 || !std::isfinite(pinned.x) ||
            !std::isfinite(pinned.y))
            pin = -1;
        if (pin >= 0 || pin != lastPin)
        {
            elapsed = quiet = 0;
            awake = graph.points.size() > 1;
        }
        lastPin = pin;
        if (pin >= 0)
        {
            graph.points[pin] = pinned;
            velocity[pin] = {};
        }
        if (!awake)
            return;
        elapsed += seconds;
        accumulator += std::min(seconds, .05);
        constexpr double step = 1.0 / 240;
        while (accumulator + 1e-10 >= step)
        {
            accumulator -= step;
            double loX = 0, loY = 0, hiX = 1, hiY = 1;
            for (auto p : graph.points)
            {
                loX = std::min(loX, p.x);
                loY = std::min(loY, p.y);
                hiX = std::max(hiX, p.x);
                hiY = std::max(hiY, p.y);
            }
            tree.clear();
            std::iota(order.begin(), order.end(), 0);
            cell(0, order.size(), loX - 1, loY - 1, std::max(hiX - loX, hiY - loY) + 2, 0);
            for (std::size_t i = 0; i < graph.points.size(); ++i)
            {
                double hub = i < graph.weights.size() ? graph.weights[i] : 0;
                double center = (.0015 + .004 * hub * hub) * options.center / 100.0;
                force[i] = {-graph.points[i].x * center, -graph.points[i].y * center};
                visit(i, 0);
            }
            for (auto e : graph.edges)
            {
                if (e.a >= graph.points.size() || e.b >= graph.points.size() || e.a == e.b)
                    continue;
                Point d{graph.points[e.b].x - graph.points[e.a].x, graph.points[e.b].y - graph.points[e.a].y};
                double distance = std::max(.01, std::hypot(d.x, d.y));
                double target = std::max(radius(e.a) + radius(e.b) + 4,
                                         (32 + 125 * (1 - e.strength)) * options.distance / 100.0);
                double spring =
                    (distance - target) / distance * (.01 + .035 * e.strength) * options.link / 100.0;
                // A hub pulls its satellites without accelerating itself once per edge.
                double bias = degree[e.a] / (degree[e.a] + degree[e.b]);
                force[e.a].x += d.x * spring * (1 - bias);
                force[e.a].y += d.y * spring * (1 - bias);
                force[e.b].x -= d.x * spring * bias;
                force[e.b].y -= d.y * spring * bias;
            }
            double energy = 0;
            double cooling = std::clamp((12 - elapsed) / 4, 0.0, 1.0);
            for (std::size_t i = 0; i < graph.points.size(); ++i)
            {
                if (int(i) == pin)
                    continue;
                // Integrator units are 60 Hz steps, subdivided for refresh independence.
                velocity[i].x = (velocity[i].x + force[i].x * .25 * cooling) * .94;
                velocity[i].y = (velocity[i].y + force[i].y * .25 * cooling) * .94;
                double speed = std::hypot(velocity[i].x, velocity[i].y);
                if (speed > 10)
                {
                    velocity[i].x *= 10 / speed;
                    velocity[i].y *= 10 / speed;
                }
                graph.points[i].x += velocity[i].x * .25;
                graph.points[i].y += velocity[i].y * .25;
                energy += speed * speed;
            }
            quiet = energy / graph.points.size() < .001 ? quiet + step : 0;
            if (pin < 0 && (quiet > .5 || elapsed > 12))
            {
                awake = false;
                accumulator = 0;
                break;
            }
        }
    }
    void release(std::size_t index, Point point)
    {
        if (index >= graph.points.size())
            return;
        // Commit the last pointer position and clear its momentum before unpinning.
        graph.points[index] = point;
        velocity[index] = {};
        if (lastPin == int(index))
            lastPin = -1;
        elapsed = quiet = 0;
        awake = graph.points.size() > 1;
    }
};

struct GraphFrame
{
    std::vector<Point> points;
    bool awake = false;
    unsigned generation = 0;
    double age = 0;
};

// One coalesced request, bounded final drops and one replaceable result. The UI
// never waits for physics, and hiding the pane stops requests entirely.
class GraphMotion
{
    std::mutex mutex;
    std::atomic<std::uint64_t> workRevision{0};
    std::thread worker;
    bool quitting = false, pending = false, resetting = false, finish = false;
    std::atomic<unsigned> requestedGeneration{0};
    Graph seed;
    GraphForces options;
    unsigned generation = 0;
    double seconds = 0;
    int pin = -1;
    Point pinned{};
    std::uint64_t pinSequence = 0;
    struct Release
    {
        Point point;
        std::uint64_t sequence;
    };
    // Only enqueue/swap/publication use this lock, never physics or graph copies.
    // Coalescing by node bounds storage even when the worker is still settling.
    std::mutex releaseMutex;
    std::unordered_map<std::size_t, Release> releases;
    std::size_t nodeCount = 0;
    std::atomic<std::uint64_t> commandSequence{0};
    std::atomic<bool> releasePending{false};
    std::uint64_t releaseRevision = 0;
    std::atomic<std::shared_ptr<const GraphFrame>> output;

  public:
    ~GraphMotion()
    {
        stop();
    }
    void start()
    {
        worker = std::thread([this] {
            SetThreadPriority(GetCurrentThread(), THREAD_PRIORITY_BELOW_NORMAL);
            GraphSimulation simulation;
            std::vector<std::uint64_t> lastReleased;
            std::unique_lock lock(mutex);
            while (!quitting)
            {
                auto work = workRevision.load();
                if (!pending && !releasePending.load())
                {
                    lock.unlock();
                    workRevision.wait(work);
                    lock.lock();
                    continue;
                }
                if (quitting)
                    break;
                bool reset = resetting;
                bool settle = finish;
                Graph value;
                if (reset)
                    value = std::move(seed);
                auto settings = options;
                auto epoch = generation;
                auto dt = seconds;
                auto held = pin;
                auto point = pinned;
                auto heldSequence = pinSequence;
                pending = resetting = finish = false;
                seconds = 0;
                lock.unlock();
                if (reset)
                {
                    simulation.reset(std::move(value), settings);
                    lastReleased.assign(simulation.graph.points.size(), 0);
                }
                std::unordered_map<std::size_t, Release> dropped;
                std::uint64_t revision;
                {
                    std::lock_guard releaseLock(releaseMutex);
                    // A reset cancels all commands and results for the previous graph.
                    if (epoch == requestedGeneration.load())
                        dropped.swap(releases);
                    revision = releaseRevision;
                    releasePending = !releases.empty();
                }
                for (const auto &[index, released] : dropped)
                    if (index < lastReleased.size())
                    {
                        simulation.release(index, released.point);
                        lastReleased[index] = released.sequence;
                    }
                if (held >= 0 && std::size_t(held) < lastReleased.size() &&
                    heldSequence <= lastReleased[held])
                    held = -1;
                simulation.advance(dt, held, point);
                if (settle && dropped.empty())
                    for (int i = 0; i < 730 && simulation.awake && requestedGeneration == epoch &&
                                    !releasePending.load(); ++i)
                        simulation.advance(1.0 / 60);
                auto frame = std::make_shared<GraphFrame>(
                    GraphFrame{simulation.graph.points, simulation.awake, epoch, simulation.age()});
                lock.lock();
                {
                    std::lock_guard releaseLock(releaseMutex);
                    if (!quitting && epoch == generation && revision == releaseRevision)
                        output.store(std::move(frame));
                }
            }
        });
    }
    unsigned reset(const Graph &graph, GraphForces settings, bool settle = false)
    {
        Graph value = graph;
        std::lock_guard lock(mutex);
        seed = std::move(value);
        options = settings;
        ++generation;
        {
            std::lock_guard releaseLock(releaseMutex);
            requestedGeneration = generation;
            nodeCount = graph.points.size();
            releases.clear();
            releasePending = false;
            ++releaseRevision;
            output.store(nullptr);
        }
        finish = settle;
        resetting = pending = true;
        seconds = 0;
        pin = -1;
        ++workRevision;
        workRevision.notify_one();
        return generation;
    }
    bool request(double dt, int held, Point point)
    {
        std::unique_lock lock(mutex, std::try_to_lock);
        if (!lock || quitting)
            return false;
        seconds = std::min(.25, seconds + std::max(0.0, dt));
        pin = held;
        pinned = point;
        pinSequence = ++commandSequence;
        pending = true;
        ++workRevision;
        workRevision.notify_one();
        return true;
    }
    bool release(int index, Point point)
    {
        if (index < 0 || !std::isfinite(point.x) || !std::isfinite(point.y))
            return false;
        std::lock_guard releaseLock(releaseMutex);
        if (std::size_t(index) >= nodeCount)
            return false;
        releases.insert_or_assign(std::size_t(index), Release{point, ++commandSequence});
        ++releaseRevision;
        releasePending = true;
        output.store(nullptr);
        ++workRevision;
        workRevision.notify_one();
        return true;
    }
    std::shared_ptr<const GraphFrame> take()
    {
        return output.exchange(nullptr);
    }
    void stop()
    {
        {
            std::lock_guard lock(mutex);
            quitting = true;
            ++requestedGeneration;
            ++workRevision;
            workRevision.notify_one();
        }
        if (worker.joinable())
            worker.join();
    }
};
} // namespace ga
