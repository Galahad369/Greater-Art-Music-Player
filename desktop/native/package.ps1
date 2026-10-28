param([Parameter(Mandatory=$true)][string]$WixPath,[string]$VcpkgRegistry)
$ErrorActionPreference='Stop'
& (Join-Path $PSScriptRoot 'build.ps1') -RequireCodecs
& (Join-Path $PSScriptRoot 'prepare-codec-package.ps1') -VcpkgRegistry $VcpkgRegistry
Push-Location $PSScriptRoot
try {
    $packageRelative='build/package-'+[Guid]::NewGuid().ToString('N')
    New-Item -ItemType Directory -Path $packageRelative | Out-Null
    $msiPath=Join-Path $packageRelative 'GreaterArt.msi'
    & (Join-Path $WixPath 'candle.exe') -nologo -arch x64 -out build/Product.wixobj Product.wxs
    if ($LASTEXITCODE -ne 0) { throw 'MSI compilation failed.' }
    & (Join-Path $WixPath 'light.exe') -nologo -out $msiPath build/Product.wixobj
    if ($LASTEXITCODE -ne 0) { throw 'MSI validation/link failed.' }
    $hash=(Get-FileHash -LiteralPath $msiPath -Algorithm SHA256).Hash.ToLowerInvariant()
    [IO.File]::WriteAllText((Join-Path $PSScriptRoot ($packageRelative+'/msi.sha256')),$hash,[Text.Encoding]::ASCII)
    $resourceHeader='#define GA_MSI_PAYLOAD "'+$packageRelative+'/GreaterArt.msi"'+[Environment]::NewLine+'#define GA_MSI_HASH "'+$packageRelative+'/msi.sha256"'+[Environment]::NewLine
    [IO.File]::WriteAllText((Join-Path $PSScriptRoot 'build/payload.rc.h'),$resourceHeader,[Text.Encoding]::ASCII)
    & rc.exe /nologo /fobuild/Setup.res Setup.rc
    if ($LASTEXITCODE -ne 0) { throw 'Setup resource compilation failed.' }
    & cl.exe /nologo /std:c++17 /EHsc /O2 /MT /W4 /utf-8 /DUNICODE /D_UNICODE Setup.cpp /Fobuild/Setup.obj /Febuild/GreaterArt-Desktop-Setup-0.1.20.exe /link /SUBSYSTEM:WINDOWS build/Setup.res msi.lib bcrypt.lib ole32.lib shell32.lib user32.lib gdi32.lib dwmapi.lib
    if ($LASTEXITCODE -ne 0) { throw 'Offline setup compilation failed.' }
    $receipt=@{version='0.1.20';code=21;msi=(Join-Path $PSScriptRoot $msiPath);msiSha256=$hash;appSha256=(Get-FileHash build/GreaterArt.exe).Hash.ToLowerInvariant();setupSha256=(Get-FileHash build/GreaterArt-Desktop-Setup-0.1.20.exe).Hash.ToLowerInvariant()}
    [IO.File]::WriteAllText((Join-Path $PSScriptRoot 'build/package-receipt.json'),($receipt|ConvertTo-Json),[Text.UTF8Encoding]::new($false))
    Write-Output 'Offline setup EXE built; desktop and Start menu shortcuts are managed by Windows Installer.'
} finally { Pop-Location }

