param([switch]$Publish, [switch]$SkipNative)
$ErrorActionPreference='Stop'
Push-Location $PSScriptRoot
try {
    if (-not $SkipNative) { & ./build-native.ps1; if ($LASTEXITCODE -ne 0) { throw 'Native engine build failed.' } }
    & dotnet restore GreaterArt.WinUI.csproj -p:Platform=x64 --locked-mode
    if ($LASTEXITCODE -ne 0) { throw 'Locked dependency restore failed.' }
    $verb=if($Publish){'publish'}else{'build'}
    & dotnet $verb GreaterArt.WinUI.csproj -c Release -p:Platform=x64 --no-restore
    if ($LASTEXITCODE -ne 0) { throw 'WinUI build failed.' }
} finally { Pop-Location }
