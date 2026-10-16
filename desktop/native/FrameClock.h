#pragma once
#include <windows.h>
#include <dwmapi.h>
#include <algorithm>
#include <atomic>
#include <condition_variable>
#include <cstdint>
#include <mutex>
#include <thread>

namespace ga
{
// Presentation pacing only. Media Foundation/WASAPI keep their own unmodified source clocks.
// A display-derived high-resolution wait never blocks the UI thread. At most
// one frame message is outstanding; idle windows sleep. No global timer change.
class FrameClock
{
    std::mutex mutex;
    std::condition_variable wake;
    std::thread worker;
    bool enabled = false, quitting = false;
    std::atomic<bool> pending{false};

  public:
    ~FrameClock()
    {
        stop();
    }
    void start(HWND window, UINT message)
    {
        worker = std::thread([this, window, message] {
            HANDLE timer = CreateWaitableTimerEx(nullptr, nullptr, CREATE_WAITABLE_TIMER_HIGH_RESOLUTION,
                                                 TIMER_MODIFY_STATE | SYNCHRONIZE);
            if (!timer)
                timer = CreateWaitableTimerEx(nullptr, nullptr, 0, TIMER_MODIFY_STATE | SYNCHRONIZE);
            LARGE_INTEGER frequency{}, counter{};
            QueryPerformanceFrequency(&frequency);
            double period = 1.0 / 60;
            std::uint64_t refreshAt = 0;
            double next = 0;
            std::unique_lock lock(mutex);
            while (!quitting)
            {
                wake.wait(lock, [&] { return quitting || enabled; });
                if (quitting)
                    break;
                lock.unlock();
                if (GetTickCount64() - refreshAt > 1000)
                {
                    MONITORINFOEX monitor{};
                    monitor.cbSize = sizeof(monitor);
                    GetMonitorInfo(MonitorFromWindow(window, MONITOR_DEFAULTTONEAREST), &monitor);
                    DEVMODE mode{};
                    mode.dmSize = sizeof(mode);
                    if (EnumDisplaySettings(monitor.szDevice, ENUM_CURRENT_SETTINGS, &mode) &&
                        mode.dmDisplayFrequency > 1)
                        period = 1.0 / mode.dmDisplayFrequency;
                    refreshAt = GetTickCount64();
                }
                QueryPerformanceCounter(&counter);
                double now = double(counter.QuadPart) / frequency.QuadPart;
                if (next < now || next > now + 2 * period)
                    next = now;
                next += period;
                LARGE_INTEGER due{};
                due.QuadPart = -std::max<LONGLONG>(1, LONGLONG((next - now) * 1e7));
                if (timer && SetWaitableTimer(timer, &due, 0, nullptr, nullptr, FALSE))
                    WaitForSingleObject(timer, 100);
                else
                    Sleep(DWORD(std::max(1.0, period * 1000)));
                lock.lock();
                if (enabled && !quitting && !pending.exchange(true))
                    if (!PostMessage(window, message, 0, 0))
                        pending = false;
            }
            if (timer)
                CloseHandle(timer);
        });
    }
    void active(bool value)
    {
        std::lock_guard lock(mutex);
        enabled = value;
        wake.notify_one();
    }
    void consume()
    {
        pending = false;
    }
    void stop()
    {
        {
            std::lock_guard lock(mutex);
            quitting = true;
            wake.notify_one();
        }
        if (worker.joinable())
            worker.join();
    }
};
} // namespace ga
