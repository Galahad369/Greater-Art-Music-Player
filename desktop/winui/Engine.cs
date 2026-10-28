using System;
using System.Runtime.InteropServices;

namespace GreaterArt.Desktop;

[StructLayout(LayoutKind.Sequential)]
internal struct PlaybackState
{
    public double Position, Duration;
    public int State, Width, Height;
    public ulong Frames, Identity;
    public int BundledAudio;
    public uint VideoError, Palette0, Palette1;
    public ulong PaletteSamples;
}

internal static class Engine
{
    private const string Library = "GreaterArt.Engine.dll";
    [DllImport(Library, CallingConvention = CallingConvention.Cdecl)] internal static extern int GA_Initialize();
    [DllImport(Library, CallingConvention = CallingConvention.Cdecl)] internal static extern void GA_Shutdown();
    [DllImport(Library, CallingConvention = CallingConvention.Cdecl)] internal static extern nint GA_Create();
    [DllImport(Library, CallingConvention = CallingConvention.Cdecl)] internal static extern void GA_Release(nint player);
    [DllImport(Library, CallingConvention = CallingConvention.Cdecl, CharSet = CharSet.Unicode)] internal static extern int GA_Load(nint player, string path);
    [DllImport(Library, CallingConvention = CallingConvention.Cdecl)] internal static extern int GA_Play(nint player);
    [DllImport(Library, CallingConvention = CallingConvention.Cdecl)] internal static extern int GA_Pause(nint player, int paused);
    [DllImport(Library, CallingConvention = CallingConvention.Cdecl)] internal static extern int GA_Seek(nint player, double seconds);
    [DllImport(Library, CallingConvention = CallingConvention.Cdecl)] internal static extern int GA_Rate(nint player, double rate);
    [DllImport(Library, CallingConvention = CallingConvention.Cdecl)] internal static extern int GA_Volume(nint player, double volume);
    [DllImport(Library, CallingConvention = CallingConvention.Cdecl)] internal static extern int GA_AmbientEnable(nint player, int enabled);
    [DllImport(Library, CallingConvention = CallingConvention.Cdecl)] internal static extern nint GA_CreateHost(nint parent);
    [DllImport(Library, CallingConvention = CallingConvention.Cdecl)] internal static extern int GA_SetHostBounds(nint host, int x, int y, int width, int height, int visible);
    [DllImport(Library, CallingConvention = CallingConvention.Cdecl)] internal static extern void GA_DestroyHost(nint host);
    [DllImport(Library, CallingConvention = CallingConvention.Cdecl)] internal static extern int GA_Attach(nint player, nint host);
    [DllImport(Library, CallingConvention = CallingConvention.Cdecl)] internal static extern int GA_GetState(nint player, out PlaybackState state);
    [DllImport(Library, CallingConvention = CallingConvention.Cdecl, CharSet = CharSet.Unicode)] internal static extern int GA_CaptureHost(nint host, string path);

    internal static void Check(int result)
    {
        if (result < 0) Marshal.ThrowExceptionForHR(result);
    }
}
