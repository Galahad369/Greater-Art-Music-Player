param([Parameter(Mandatory)][string[]]$VerificationMedia)
$ErrorActionPreference = 'Stop'
$project = [xml](Get-Content -LiteralPath (Join-Path $PSScriptRoot 'GreaterArt.WinUI.csproj') -Raw)
$version = $project.Project.PropertyGroup.Version
$releaseDirectory = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../releases'))
$archive = Join-Path $releaseDirectory ('GreaterArt-WinUI-Preview-' + $version + '-x64.zip')
$receiptPath = $archive + '.json'
if ((Test-Path -LiteralPath $archive) -or (Test-Path -LiteralPath $receiptPath)) { throw 'This version already has a frozen artifact. Do not overwrite it.' }
& (Join-Path $PSScriptRoot 'build.ps1') -Publish
if ($LASTEXITCODE -ne 0) { throw 'Publish failed.' }
$publish = Join-Path $PSScriptRoot 'bin/x64/Release/net10.0-windows10.0.26100.0/win-x64/publish'
$executable = Join-Path $publish 'GreaterArt.WinUI.exe'
$evidence = Join-Path $PSScriptRoot ('artifacts/package-' + [guid]::NewGuid().ToString('N'))
& (Join-Path $PSScriptRoot 'verify.ps1') -Executable $executable -Media $VerificationMedia -OutputDirectory $evidence
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'README.md') -Destination $publish
New-Item -ItemType Directory -Path $releaseDirectory -Force | Out-Null
Compress-Archive -Path (Join-Path $publish '*') -DestinationPath $archive -CompressionLevel Optimal
$receipt = [ordered]@{
    version=$version
    state='LOCAL_WINUI_PREVIEW_NOT_FULL_PARITY'
    createdUtc=[DateTime]::UtcNow.ToString('O')
    archive=(Split-Path $archive -Leaf)
    archiveSha256=(Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash
    executableSha256=(Get-FileHash -LiteralPath $executable -Algorithm SHA256).Hash
    managedAssemblySha256=(Get-FileHash -LiteralPath (Join-Path $publish 'GreaterArt.WinUI.dll') -Algorithm SHA256).Hash
    engineSha256=(Get-FileHash -LiteralPath (Join-Path $publish 'GreaterArt.Engine.dll') -Algorithm SHA256).Hash
    verification=(Get-Content -LiteralPath (Join-Path $evidence 'verification.json') -Raw | ConvertFrom-Json)
    limits='Unsigned local preview. Clean-machine, full accessibility, physical listening and complete desktop parity are not verified.'
}
$receipt | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath $receiptPath -Encoding UTF8
Write-Output ('Portable preview: ' + $archive)
Write-Output ('Run: ' + $executable)
