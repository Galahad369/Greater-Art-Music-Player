#pragma once
#include <cstdio>
#include <string>
#include <windows.h>
namespace ga
{
inline wchar_t crashFile[32768]{};
inline LONG WINAPI writeCrash(EXCEPTION_POINTERS *exception)
{
    HANDLE file =
        CreateFileW(crashFile, GENERIC_WRITE, FILE_SHARE_READ, nullptr, CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, nullptr);
    if (file != INVALID_HANDLE_VALUE)
    {
        char message[160]{};
        sprintf_s(message, "Greater Art native crash\nexception=0x%08lX\nNo media paths or memory dump recorded.\n",
                  exception && exception->ExceptionRecord ? exception->ExceptionRecord->ExceptionCode : 0);
        DWORD written = 0;
        WriteFile(file, message, DWORD(strlen(message)), &written, nullptr);
        CloseHandle(file);
    }
    return EXCEPTION_EXECUTE_HANDLER;
}
inline void enableCrashRecord(const std::wstring &directory)
{
    auto path = directory + L"\\crash-latest.txt";
    if (path.size() < 32768)
        wcscpy_s(crashFile, path.c_str());
    SetUnhandledExceptionFilter(writeCrash);
}
} // namespace ga
