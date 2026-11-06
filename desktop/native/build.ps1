param([switch]$RequireCodecs)
$ErrorActionPreference = 'Stop'
$vswhere = Join-Path ${env:ProgramFiles(x86)} 'Microsoft Visual Studio/Installer/vswhere.exe'
$installation = & $vswhere -latest -products '*' -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath
if (-not $installation) { throw 'Visual Studio C++ desktop workload and Windows SDK are required to build, not to run the app.' }
Import-Module (Join-Path $installation 'Common7/Tools/Microsoft.VisualStudio.DevShell.dll')
Enter-VsDevShell -VsInstallPath $installation -SkipAutomaticLocation -DevCmdArguments '-arch=x64 -host_arch=x64' | Out-Null
Push-Location $PSScriptRoot
try {
    New-Item -ItemType Directory -Force -Path build | Out-Null
    $sources=@('App.cpp','Icons.cpp','Media.cpp','VideoAmbient.cpp','AudioAnalysis.cpp','AccessibilityGuids.cpp','CodecReader.cpp','AudioFallback.cpp','StackPcm.cpp','StackPcmTests.cpp','StackOutputTests.cpp')
    $options=@('/nologo','/std:c++20','/EHsc','/O2','/MT','/W4','/utf-8','/DUNICODE','/D_UNICODE')
    $libraries=@('mfreadwrite.lib','mfplat.lib','mf.lib','mfuuid.lib','ole32.lib','oleaut32.lib','user32.lib','gdi32.lib','propsys.lib','shell32.lib','comctl32.lib','dwmapi.lib','gdiplus.lib','msimg32.lib','oleacc.lib','windowsapp.lib','uuid.lib','mmdevapi.lib','avrt.lib')
    $codecs=Join-Path $PSScriptRoot 'build/codecs/installed/greater-art-x64-windows'
    if(Test-Path (Join-Path $codecs 'lib/avcodec.lib')) {
        $options+=@('/DGA_HAS_CODECS=1',('/I'+(Join-Path $codecs 'include')))
        foreach($name in @('avcodec','avformat','avutil','swresample','avfilter')){$libraries+=(Join-Path $codecs ('lib/'+$name+'.lib'))}
        foreach($name in @('avcodec-63.dll','avformat-63.dll','avutil-61.dll','swresample-7.dll','avfilter-12.dll','swscale-10.dll')){
            Copy-Item -LiteralPath (Join-Path $codecs ('bin/'+$name)) -Destination (Join-Path $PSScriptRoot ('build/'+$name))
        }
    } elseif($RequireCodecs) { throw 'Approved source-built codec runtime is missing. Run build-codecs.ps1 first.' }
    & cl.exe @options @sources /Fobuild/ /Febuild/GreaterArt-raw.exe /link /SUBSYSTEM:WINDOWS @libraries
    if ($LASTEXITCODE -ne 0) { throw 'Native app compilation failed.' }
    $iconStage = New-Item -ItemType Directory -Path ('build/icon-' + [guid]::NewGuid().ToString('N'))
    $stagedIcon = Join-Path $iconStage.FullName 'app.ico'
    $iconProcess = Start-Process -FilePath (Join-Path $PSScriptRoot 'build/GreaterArt-raw.exe') -ArgumentList @('--write-icon', 'app.ico') -WorkingDirectory $iconStage.FullName -WindowStyle Hidden -PassThru -Wait
    if ($iconProcess.ExitCode -ne 0 -or !(Test-Path -LiteralPath $stagedIcon -PathType Leaf) -or
        (Get-Item -LiteralPath $stagedIcon).Length -eq 0) {
        throw "App icon generation failed. Staged path: $stagedIcon"
    }
    $icon = Join-Path $PSScriptRoot 'build/app.ico'
    $priorIcon = Join-Path $iconStage.FullName 'app.ico.prior'
    if ([IO.File]::Exists($icon)) {
        if ((Get-Item -LiteralPath $icon -Force).Attributes -band [IO.FileAttributes]::ReparsePoint) {
            throw "App icon destination is a reparse point: $icon"
        }
        [IO.File]::Replace($stagedIcon, $icon, $priorIcon)
        Remove-Item -LiteralPath $priorIcon -ErrorAction SilentlyContinue
    } else {
        [IO.File]::Move($stagedIcon, $icon)
    }
    if (!(Test-Path -LiteralPath $priorIcon)) {
        [IO.Directory]::Delete($iconStage.FullName)
    }
    & rc.exe /nologo /fobuild/App.res App.rc
    if ($LASTEXITCODE -ne 0) { throw 'Native resource compilation failed.' }
    $objects=$sources|ForEach-Object {'build/'+[IO.Path]::GetFileNameWithoutExtension($_)+'.obj'}
    & link.exe /nologo /OUT:build/GreaterArt.exe /SUBSYSTEM:WINDOWS @objects build/App.res @libraries
    if ($LASTEXITCODE -ne 0) { throw 'Native app linking failed.' }
    Write-Output 'Built original Windows-native GreaterArt.exe. No VLC, Java or Electron runtime.'
} finally { Pop-Location }

