using System;
using System.ComponentModel;
using System.IO;
using System.Runtime.CompilerServices;

namespace GreaterArt.Desktop;

public sealed class LibraryTrack : INotifyPropertyChanged
{
    public string Path { get; }
    public string Title { get; }
    public string Detail { get; }
    private bool current;
    public string Marker => current ? "▌" : "";
    public bool IsCurrent { get => current; set { if (current == value) return; current = value; Changed(nameof(Marker)); Changed(); } }
    public LibraryTrack(string path)
    {
        Path = path;
        Title = System.IO.Path.GetFileNameWithoutExtension(path);
        var info = new FileInfo(path);
        Detail = $"{info.Extension.TrimStart('.').ToUpperInvariant()}  ·  {info.Length / 1048576d:0.##} MB";
    }
    public event PropertyChangedEventHandler? PropertyChanged;
    private void Changed([CallerMemberName] string? property = null) => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(property));
}
