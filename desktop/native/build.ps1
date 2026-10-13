$ErrorActionPreference = 'Stop'
$vswhere = Join-Path ${env:ProgramFiles(x86)} 'Microsoft Visual Studio/Installer/vswhere.exe'
$installation = & $vswhere -latest -products '*' -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath
if (-not $installation) { throw 'Visual Studio C++ desktop workload and Windows SDK are required to build, not to run the app.' }
Import-Module (Join-Path $installation 'Common7/Tools/Microsoft.VisualStudio.DevShell.dll')
Enter-VsDevShell -VsInstallPath $installation -SkipAutomaticLocation -DevCmdArguments '-arch=x64 -host_arch=x64' | Out-Null
Push-Location $PSScriptRoot
try {
    New-Item -ItemType Directory -Force -Path build | Out-Null
    & cl.exe /nologo /std:c++17 /EHsc /O2 /MT /W4 /utf-8 /DUNICODE /D_UNICODE App.cpp Media.cpp /Fobuild/ /Febuild/GreaterArt-raw.exe /link /SUBSYSTEM:WINDOWS mfplat.lib mf.lib mfuuid.lib ole32.lib oleaut32.lib user32.lib gdi32.lib propsys.lib shell32.lib comctl32.lib dwmapi.lib gdiplus.lib
    if ($LASTEXITCODE -ne 0) { throw 'Native app compilation failed.' }
    $iconProcess = Start-Process -FilePath (Join-Path $PSScriptRoot 'build/GreaterArt-raw.exe') -ArgumentList @('--write-icon', 'build/app.ico') -WindowStyle Hidden -PassThru -Wait
    if ($iconProcess.ExitCode -ne 0) { throw 'App icon generation failed.' }
    & rc.exe /nologo /fobuild/App.res App.rc
    if ($LASTEXITCODE -ne 0) { throw 'Native resource compilation failed.' }
    & link.exe /nologo /OUT:build/GreaterArt.exe /SUBSYSTEM:WINDOWS build/App.obj build/Media.obj build/App.res mfplat.lib mf.lib mfuuid.lib ole32.lib oleaut32.lib user32.lib gdi32.lib propsys.lib shell32.lib comctl32.lib dwmapi.lib gdiplus.lib
    if ($LASTEXITCODE -ne 0) { throw 'Native app linking failed.' }
    Write-Output 'Built original Windows-native GreaterArt.exe. No VLC, Java or Electron runtime.'
} finally { Pop-Location }

