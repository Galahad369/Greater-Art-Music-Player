param([string]$OutputDirectory)
$ErrorActionPreference = 'Stop'
$nativeRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../native'))
$buildRoot = Join-Path $PSScriptRoot 'native-build'
if (-not $OutputDirectory) { $OutputDirectory = $buildRoot }
$OutputDirectory = [IO.Path]::GetFullPath($OutputDirectory)
$vswhere = Join-Path ${env:ProgramFiles(x86)} 'Microsoft Visual Studio/Installer/vswhere.exe'
$installation = & $vswhere -latest -products '*' -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath
if (-not $installation) { throw 'Visual Studio C++ tools and the Windows SDK are required.' }
Import-Module (Join-Path $installation 'Common7/Tools/Microsoft.VisualStudio.DevShell.dll')
Enter-VsDevShell -VsInstallPath $installation -SkipAutomaticLocation -DevCmdArguments '-arch=x64 -host_arch=x64' | Out-Null
$codecs = Join-Path $nativeRoot 'build/codecs/installed/greater-art-x64-windows'
if (-not (Test-Path -LiteralPath (Join-Path $codecs 'lib/avcodec.lib'))) {
    throw 'Approved source-built codecs are missing. This bridge does not download substitute binaries.'
}
New-Item -ItemType Directory -Path $buildRoot -Force | Out-Null
New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
Push-Location $buildRoot
try {
    $sources = @((Join-Path $PSScriptRoot 'NativeBridge.cpp'), (Join-Path $PSScriptRoot 'GraphBridge.cpp'))
    foreach ($name in @('Media.cpp', 'VideoAmbient.cpp', 'CodecReader.cpp', 'AudioFallback.cpp', 'StackPcm.cpp')) {
        $sources += Join-Path $nativeRoot $name
    }
    $options = @('/nologo', '/std:c++20', '/EHsc', '/O2', '/MT', '/W4', '/utf-8', '/DUNICODE', '/D_UNICODE',
        '/DGA_HAS_CODECS=1', ('/I' + (Join-Path $codecs 'include')))
    $libraries = @('mfreadwrite.lib', 'mfplat.lib', 'mf.lib', 'mfuuid.lib', 'ole32.lib', 'oleaut32.lib',
        'user32.lib', 'gdi32.lib', 'propsys.lib', 'shell32.lib', 'dwmapi.lib', 'uuid.lib', 'mmdevapi.lib', 'avrt.lib',
        'd3d11.lib', 'dxgi.lib', 'windowsapp.lib')
    foreach ($name in @('avcodec', 'avformat', 'avutil', 'swresample', 'avfilter')) {
        $libraries += Join-Path $codecs ('lib/' + $name + '.lib')
    }
    $engine = Join-Path $buildRoot 'GreaterArt.Engine.dll'
    & cl.exe @options /LD @sources /Fo./ ('/Fe' + $engine) /link @libraries
    if ($LASTEXITCODE -ne 0) { throw 'Native WinUI engine bridge compilation failed.' }
    if ($OutputDirectory -ne $buildRoot) { Copy-Item -LiteralPath $engine -Destination $OutputDirectory }
    foreach ($name in @('avcodec-63.dll', 'avformat-63.dll', 'avutil-61.dll', 'swresample-7.dll', 'avfilter-12.dll', 'swscale-10.dll')) {
        Copy-Item -LiteralPath (Join-Path $codecs ('bin/' + $name)) -Destination $OutputDirectory
    }
    # Keep the existing exact corresponding source archive and license notice.
    $notices = Join-Path $OutputDirectory 'Codecs'
    New-Item -ItemType Directory -Path $notices -Force | Out-Null
    foreach ($name in @('COPYRIGHT.txt', 'NOTICE.md', 'CodecSource.zip', 'BUILD-RECEIPT.json')) {
        $source = Join-Path $nativeRoot ('build/codecs/' + $name)
        if (-not (Test-Path -LiteralPath $source)) { throw ('Codec redistribution evidence missing: ' + $name) }
        Copy-Item -LiteralPath $source -Destination $notices
    }
    Write-Output ('Built native playback bridge: ' + (Join-Path $OutputDirectory 'GreaterArt.Engine.dll'))
} finally { Pop-Location }
