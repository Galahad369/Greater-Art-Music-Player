#pragma once
#include <condition_variable>
#include <deque>
#include <functional>
#include <mutex>
#include <thread>
#include <vector>
#include <stdexcept>
#include <windows.h>
namespace ga
{
class WorkQueue
{
    std::mutex mutex;
    std::condition_variable condition;
    struct Task
    {
        std::function<void()> run, failed;
    };
    std::deque<Task> tasks;
    std::vector<std::thread> workers;
    bool stopping = false;
    const std::size_t capacity;

  public:
    explicit WorkQueue(std::size_t concurrency = 2, std::size_t pendingCapacity = 64)
        : capacity(pendingCapacity)
    {
        if (!concurrency || concurrency > 8 || !capacity)
            throw std::invalid_argument("Invalid background queue limits");
        try
        {
            for (std::size_t i = 0; i < concurrency; ++i)
                workers.emplace_back([this] {
                    SetThreadPriority(GetCurrentThread(), THREAD_PRIORITY_BELOW_NORMAL);
                    for (;;)
                    {
                        Task task;
                        {
                            std::unique_lock<std::mutex> lock(mutex);
                            condition.wait(lock, [&] { return stopping || !tasks.empty(); });
                            if (stopping)
                                return;
                            task = std::move(tasks.front());
                            tasks.pop_front();
                        }
                        try
                        {
                            task.run();
                        }
                        catch (...)
                        {
                            OutputDebugString(L"Greater Art: background task failed safely.\n");
                            try
                            {
                                if (task.failed)
                                    task.failed();
                            }
                            catch (...)
                            {
                                OutputDebugString(L"Greater Art: failure callback failed safely.\n");
                            }
                        }
                    }
                });
        }
        catch (...)
        {
            shutdown();
            throw;
        }
    }
    bool emplace_back(std::function<void()> task, std::function<void()> failed = {})
    {
        std::lock_guard<std::mutex> lock(mutex);
        // Never discard an admitted operation: its caller may be waiting for
        // completion. Rejection is explicit and never blocks the UI producer.
        if (stopping || tasks.size() >= capacity)
            return false;
        tasks.push_back({std::move(task), std::move(failed)});
        condition.notify_one();
        return true;
    }
    void shutdown()
    {
        {
            std::lock_guard<std::mutex> lock(mutex);
            stopping = true;
            tasks.clear();
        }
        condition.notify_all();
        for (auto &worker : workers)
            if (worker.joinable())
                worker.join();
    }
    ~WorkQueue()
    {
        shutdown();
    }
};
} // namespace ga
