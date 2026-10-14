#pragma once
#include <condition_variable>
#include <deque>
#include <functional>
#include <mutex>
#include <thread>
#include <vector>
#include <windows.h>
namespace ga
{
class WorkQueue
{
    std::mutex mutex;
    std::condition_variable condition;
    std::deque<std::function<void()>> tasks;
    std::vector<std::thread> workers;
    bool stopping = false;

  public:
    WorkQueue()
    {
        for (int i = 0; i < 2; ++i)
            workers.emplace_back([this] {
                SetThreadPriority(GetCurrentThread(), THREAD_PRIORITY_BELOW_NORMAL);
                for (;;)
                {
                    std::function<void()> task;
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
                        task();
                    }
                    catch (...)
                    {
                        OutputDebugString(L"Greater Art: background task failed safely.\n");
                    }
                }
            });
    }
    void emplace_back(std::function<void()> task)
    {
        std::lock_guard<std::mutex> lock(mutex);
        if (stopping)
            return;
        if (tasks.size() >= 64)
            tasks.pop_front();
        tasks.push_back(std::move(task));
        condition.notify_one();
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
