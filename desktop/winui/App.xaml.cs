using Microsoft.UI.Xaml;
using System;
using System.IO;

namespace GreaterArt.Desktop;

public partial class App : Application
{
    private MainWindow? window;

    public App()
    {
        RequestedTheme = ApplicationTheme.Dark;
        InitializeComponent();
        UnhandledException += (_, args) => RecordFailure(args.Exception);
        AppDomain.CurrentDomain.UnhandledException += (_, args) =>
            RecordFailure(args.ExceptionObject as Exception ?? new Exception("Unhandled native host failure."));
    }

    protected override void OnLaunched(LaunchActivatedEventArgs args)
    {
        window = new MainWindow();
        window.Activate();
    }

    internal static string DataFolder => Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "GreaterArtDesktop", "WinUI");

    internal static void RecordFailure(Exception error)
    {
        try
        {
            Directory.CreateDirectory(DataFolder);
            File.AppendAllText(Path.Combine(DataFolder, "failures.log"), $"{DateTimeOffset.Now:O}\n{error}\n");
        }
        catch (IOException) { }
        catch (UnauthorizedAccessException) { }
    }
}
