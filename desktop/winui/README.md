# Greater Art WinUI preview

This is the first stage of the Windows interface rebuild.
It uses WinUI 3 and the existing native playback engine.
It does not use VLC or another installed player.
Android is unchanged.

## Run

Extract the complete ZIP to a local folder.
Open `GreaterArt.WinUI.exe` in that folder.
Keep the accompanying DLL files and the `Codecs` folder.

Select **Add folder** to read your media files.
Select a track to play it in Library.
Select the dock title or **Open player** to open Now Playing.
Use the top-right player buttons for Mini and fullscreen.
Use Escape to leave fullscreen.

The app stores preview preferences separately from the old desktop app.
It does not modify your media files.

## Build and test

The build requires .NET 10, Visual Studio C++ tools and the Windows SDK.
Build the approved codec libraries with the existing native build instructions first.
Prepare their existing redistribution evidence before the WinUI build. Pass the
pinned vcpkg registry path when the port was built directly from that checkout.

```powershell
./desktop/native/prepare-codec-package.ps1 -VcpkgRegistry '<pinned-registry>'
./desktop/winui/build.ps1 -Publish
./desktop/winui/verify.ps1 -Executable '<published-folder>/GreaterArt.WinUI.exe' -Media @('<local-video>') -OutputDirectory '<new-evidence-folder>'
```

The playback test mutes audio and writes evidence to a new local directory.
It does not load or overwrite your preferences.

## Limits

This preview is not the complete desktop replacement.
The previous desktop implementation remains available.
Full feature migration, installer signing and clean-machine acceptance remain separate gates.
See `desktop/docs/WINUI_REBUILD.md` for the evidence and migration decisions.
