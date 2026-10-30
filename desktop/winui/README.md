# Greater Art WinUI preview

This is the first stage of the Windows interface rebuild.
It uses WinUI 3 and the existing native playback engine.
It does not use VLC or another installed player.
Android is unchanged.

Current locally tested preview: **0.1.21/code22**. This is not the full desktop replacement.

## Run

After a local build, open `bin/x64/Release/net10.0-windows10.0.26100.0/win-x64/publish/GreaterArt.WinUI.exe`.
If you receive a portable ZIP, extract the complete ZIP before opening its EXE.
Keep the accompanying DLL files and the `Codecs` folder.

Select **Add folder** to read your media files.
Select a track to play it in Library.
Select the dock title or **Open player** to open Now Playing.
Use the top-right player buttons for Mini and fullscreen.
Use Escape to leave fullscreen.

The app stores preview preferences separately from the old desktop app.
It does not modify your media files.

## Nodes

Open **Nodes** after you add a media folder. Links use filename similarity only.
Open the gear for **Filters**, **Groups**, **Display** and **Forces**.
Filename queries match all space-separated terms. They do not use Obsidian's note-query syntax.
Select a visible track and enable **Only this neighborhood** to show one to three connection steps.
Groups color matching filenames; the first matching group wins.

Drag a node to move it. Drag empty space, use the middle mouse button, or hold Space and drag to pan.
Scroll to zoom around the pointer. Arrow keys pan; + and − zoom; Home fits; Enter plays the selected track.
Escape closes the settings panel. Names appear as you zoom in; selection and playback remain marked.
The wand reheats the existing layout. It respects the Windows animation setting.
Graph settings are saved separately from the older native app.

See [verification and remaining limits](VERIFICATION_0.1.21.md).

## Build and test

The build requires .NET 10, Visual Studio C++ tools and the Windows SDK.
Build the approved codec libraries with the existing native build instructions first.
Prepare their existing redistribution evidence before the WinUI build. Pass the
pinned vcpkg registry path when the port was built directly from that checkout.

```powershell
./desktop/native/prepare-codec-package.ps1 -VcpkgRegistry '<pinned-registry>'
./desktop/winui/build.ps1 -Publish
dotnet run --project desktop/winui/tests/GraphExploration.Tests.csproj -c Release
./desktop/winui/test-graph.ps1
./desktop/winui/verify.ps1 -Executable '<published-folder>/GreaterArt.WinUI.exe' -Media @('<local-video>') -OutputDirectory '<new-evidence-folder>'
```

The playback test mutes audio and writes evidence to a new local directory.
It does not load or overwrite your preferences.

## Limits

This preview is not the complete desktop replacement.
The previous desktop implementation remains available.
Full feature migration, installer signing and clean-machine acceptance remain separate gates.
See `desktop/docs/WINUI_REBUILD.md` for the evidence and migration decisions.
