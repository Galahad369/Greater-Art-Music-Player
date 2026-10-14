param([Parameter(Mandatory=$true)][string]$VcpkgExe)
$ErrorActionPreference='Stop'
$nativeRoot=[IO.Path]::GetFullPath($PSScriptRoot)
$buildRoot=Join-Path $nativeRoot 'build/codecs'
New-Item -ItemType Directory -Force -Path $buildRoot | Out-Null
$drive=$null
try {
    if($nativeRoot.Contains(' ')) {
        foreach($letter in @('R','S','T','U','V','W','X','Y','Z')){
            if([IO.DriveInfo]::GetDrives().Name -contains ($letter+':\')){continue}
            & subst.exe ($letter+':') $nativeRoot
            if($LASTEXITCODE -eq 0){$drive=$letter+':';break}
        }
        if(-not $drive){throw 'No free drive letter for the temporary, non-copying codec-build mapping.'}
        $nativeRoot=$drive+'/'
        $buildRoot=Join-Path $nativeRoot 'build/codecs'
    }
    $env:VCPKG_DOWNLOADS=Join-Path $buildRoot 'downloads'
    $env:VCPKG_DEFAULT_BINARY_CACHE=Join-Path $buildRoot 'binary-cache'
    New-Item -ItemType Directory -Force -Path $env:VCPKG_DOWNLOADS,$env:VCPKG_DEFAULT_BINARY_CACHE | Out-Null
    $env:VCPKG_MAX_CONCURRENCY='2'
    & $VcpkgExe install --triplet greater-art-x64-windows --overlay-triplets (Join-Path $nativeRoot 'triplets') --x-manifest-root $nativeRoot --x-install-root (Join-Path $buildRoot 'installed') --x-buildtrees-root (Join-Path $buildRoot 'buildtrees') --x-packages-root (Join-Path $buildRoot 'packages') --disable-metrics
    if($LASTEXITCODE -ne 0){throw 'Traceable codec build failed. Do not substitute the broad prebuilt archive.'}
} finally {
    if($drive){
        $mapping=@(& subst.exe)|Where-Object {$_ -like ($drive+'*')}
        if($mapping -and $mapping.TrimEnd().EndsWith([IO.Path]::GetFullPath($PSScriptRoot),[StringComparison]::OrdinalIgnoreCase)){& subst.exe $drive /D}
        else {Write-Warning 'Build drive mapping changed externally; it was not removed.'}
    }
}
Write-Output 'Codec source/build evidence is retained in ignored build/codecs. Source/license acceptance and runtime tests still gate redistribution.'
