# Desktop 0.1.1 / code2 — local verified delivery

October 13, 2026. Development build, Windows x64. Native source commit:
`be22da9c48bb1523270506fa7cb958e35755f838` on the existing `feat/greater-art-windows-desktop` branch.
Local commit only; no public release, push or main merge. Android source/runtime/APK are unchanged by this commit.

## Frozen artifacts

| Local artifact | Bytes | SHA-256 |
| --- | ---: | --- |
| releases/GreaterArt-Desktop-Setup-0.1.1.exe | 647168 | a6d5d54186173e8087c660d07f0d94659f354c975efc352514595c3737a4714a |
| releases/GreaterArt-Desktop-0.1.1.exe | 582656 | ccf14d7a7c8babcd5e04ae230ffb7fb48461583dd6d1316fac277b502a3f3aa3 |

Both resource versions are 0.1.1. These are ignored local artifacts, not GitHub downloads. They were copied without overwriting another release. Setup embeds MSI SHA-256 `6337998e9af94e45f9ef2266d7ac85c848716721725623983ea1dec88da82ff2`.

Run Setup and keep **Create desktop shortcut** checked. It is also already installed for this user at `%LOCALAPPDATA%/Programs/GreaterArtDesktop/GreaterArt.exe`; Desktop and Start menu Programs/Greater Art Desktop shortcuts were checked against that exact target.

## Final evidence

- Rebuilt from clean committed source, then installed; installed executable hash equals the frozen app hash.
- Native self-tests: **18/18** (`native/build/committed-tests/unit-tests.txt`).
- Desktop guard tests: **5/5**; independent Android/desktop version-policy regression tests: **4/4**.
- Full desktop history/identity guard from 884a2d5 through source commit: passed.
- Installed video probe (`native/build/verify-committed/verification.txt`): PASS, six actual playing media engines, generated 1280×720 fixture, **234 renderer frames**, exact monitor fullscreen bounds and dock/expanded/Mini transfers.
- Native app imports MFPlat, COM, user32/GDI, shell, common controls, DWM, GDI+ and kernel Windows libraries. Setup imports Windows Installer/BCrypt and Windows UI/shell libraries. No VLC/JVM/Electron DLL runtime, downloader or application networking added.
- Uninstall of only known 0.1.1 test product registrations removed the executable and both shortcuts; reinstall recreated both. User settings/media were retained.
- WiX reports ICE91 for the intentionally per-user LocalAppData installation; it is not a per-machine installer. No MSI validation errors.

## Limitations

Unsigned development binaries; embedded checksum does not authenticate a publisher. No instruction to disable SmartScreen/antivirus. This workstation is tested, not a clean Windows acceptance matrix. Do not call it crash-free or security-certified.

Six copies of a generated fixture prove concurrent playback, NOT audible alignment of different singers. Stack is still independent engines, not a shared sample clock. Full Android parity, native control accessibility/Narrator, translations, artwork/waveform/lyrics/Ambient/history and mixed-DPI/multi-monitor acceptance remain pending. See HANDOFF.md and PARITY.md.
