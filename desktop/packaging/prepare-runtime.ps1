# Explicit development packaging step. The installed application never downloads code.
$ErrorActionPreference = 'Stop'
$desktopRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$archiveRoot = Join-Path $desktopRoot 'build/native-runtime'
$resourcesRoot = Join-Path $desktopRoot 'app/runtime/windows-x64'
$runtimeRoot = Join-Path $resourcesRoot 'vlc'
$expectedHash = 'fcf30850371ad10c9373cc4f0f4501e7dee49e3e9ae9f20c72fb2661a1ca6323'
New-Item -ItemType Directory -Force -Path $archiveRoot, $resourcesRoot | Out-Null
$archive = Join-Path $archiveRoot 'vlc-3.0.24-win64.zip'
if (-not (Test-Path -LiteralPath $archive) -or (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expectedHash) {
    # Resume interrupted build downloads, but never trust them until the pinned
    # vendor checksum passes. This destination is ignored build output, not a release.
    & curl.exe --fail --location --retry 3 --retry-all-errors --retry-delay 2 --connect-timeout 20 --max-time 300 --continue-at - --output $archive 'https://download.videolan.org/pub/videolan/vlc/3.0.24/win64/vlc-3.0.24-win64.zip'
    if ($LASTEXITCODE -ne 0) { throw 'Runtime download failed. No archive was extracted.' }
}
if ((Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expectedHash) {
    throw 'VLC archive checksum mismatch. Nothing was extracted.'
}
if (Test-Path -LiteralPath $runtimeRoot) {
    throw 'Runtime already exists; refusing to replace it. Inspect it before preparing a new runtime.'
}
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead($archive)
try {
    foreach ($entry in $zip.Entries) {
        $resolved = [IO.Path]::GetFullPath((Join-Path $archiveRoot $entry.FullName))
        if (-not $resolved.StartsWith($archiveRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
            throw 'Archive entry escapes its extraction root.'
        }
    }
} finally { $zip.Dispose() }
$extracted = Join-Path $archiveRoot 'vlc-3.0.24'
if (Test-Path -LiteralPath $extracted) { throw 'Extraction destination already exists; refusing overwrite.' }
Expand-Archive -LiteralPath $archive -DestinationPath $archiveRoot
if (-not (Test-Path -LiteralPath (Join-Path $extracted 'libvlc.dll'))) { throw 'Unexpected archive layout.' }
# Copy the complete vendor runtime, including plugin and license files, without stripping.
Copy-Item -LiteralPath $extracted -Destination $runtimeRoot -Recurse
Write-Output "Verified development runtime prepared: $runtimeRoot"
Write-Output "SHA-256: $expectedHash"
Write-Output 'Public distribution remains gated on licensing and signing review.'
