$ErrorActionPreference='Stop'
$nativeRoot=[IO.Path]::GetFullPath($PSScriptRoot)
$codecRoot=Join-Path $nativeRoot 'build/codecs'
$installed=Join-Path $codecRoot 'installed/greater-art-x64-windows'
$trees=@(Get-ChildItem -LiteralPath (Join-Path $codecRoot 'buildtrees/ffmpeg/src') -Directory | Where-Object {$_.Name -like 'n9.0.2-*.clean'})
if($trees.Count -ne 1){throw 'Exact patched FFmpeg 9.0.2 source tree is required.'}
$sourceArchive=Join-Path $codecRoot 'downloads/ffmpeg-ffmpeg-n9.0.2.tar.gz'
$license=Join-Path $installed 'share/ffmpeg/copyright'
$configuration=Join-Path $codecRoot 'buildtrees/ffmpeg/greater-art-x64-windows-rel'
$portRoot=Join-Path $env:LOCALAPPDATA 'vcpkg/registries/git-trees/b9e158fe3e4f9d727af5e5904e1121d844267df3'
foreach($required in @($sourceArchive,$license,(Join-Path $configuration 'build.sh'),(Join-Path $trees[0].FullName 'COPYING.LGPLv2.1'),(Join-Path $portRoot 'portfile.cmake'))){if(-not (Test-Path -LiteralPath $required)){throw ('Missing codec source evidence: '+$required)}}
$configText=Get-Content -LiteralPath (Join-Path $configuration 'config.h') -Raw
foreach($flag in @('GPL','GPLV3','NONFREE')){if($configText -notmatch ('(?m)^#define CONFIG_'+$flag+' 0\s*$')){throw ('Unapproved codec license flag '+$flag)}}
$docs=Join-Path $nativeRoot 'build/CodecDocs'
New-Item -ItemType Directory -Force -Path $docs | Out-Null
$sourceStage=Join-Path $codecRoot ('source-package-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $sourceStage | Out-Null
Copy-Item -LiteralPath $trees[0].FullName -Destination (Join-Path $sourceStage 'ffmpeg-9.0.2') -Recurse
New-Item -ItemType Directory -Path (Join-Path $sourceStage 'build-evidence'),(Join-Path $sourceStage 'vcpkg-port') | Out-Null
Copy-Item -Path (Join-Path $portRoot '*') -Destination (Join-Path $sourceStage 'vcpkg-port') -Recurse
foreach($name in @('build.sh','config.h','config_components.h','config.asm','config_components.asm','cflags.rsp')){
    $inputFile=Join-Path $configuration $name
    if(Test-Path -LiteralPath $inputFile){Copy-Item -LiteralPath $inputFile -Destination (Join-Path $sourceStage 'build-evidence')}
}
foreach($name in @('vcpkg.json','build-codecs.ps1','CODECS.md')){Copy-Item -LiteralPath (Join-Path $nativeRoot $name) -Destination $sourceStage}
Copy-Item -LiteralPath (Join-Path $nativeRoot 'triplets') -Destination $sourceStage -Recurse
Copy-Item -LiteralPath (Join-Path $codecRoot 'buildtrees/ffmpeg/build-greater-art-x64-windows-rel-config.log') -Destination (Join-Path $sourceStage 'build-evidence')
Copy-Item -LiteralPath $license -Destination (Join-Path $docs 'COPYRIGHT.txt')
Copy-Item -LiteralPath (Join-Path $nativeRoot 'CODECS.md') -Destination (Join-Path $docs 'NOTICE.md')
$dlls=@('avcodec-63.dll','avformat-63.dll','avutil-61.dll','swresample-7.dll','avfilter-12.dll','swscale-10.dll')
$runtime=@{};foreach($name in $dlls){$inputFile=Join-Path $installed ('bin/'+$name);Copy-Item -LiteralPath $inputFile -Destination (Join-Path $nativeRoot 'build');$runtime[$name]=(Get-FileHash -LiteralPath $inputFile -Algorithm SHA256).Hash.ToLowerInvariant()}
$zipPath=Join-Path $codecRoot ('CodecSource-'+[Guid]::NewGuid().ToString('N')+'.zip')
Compress-Archive -LiteralPath $sourceStage -DestinationPath $zipPath -CompressionLevel Optimal
Copy-Item -LiteralPath $zipPath -Destination (Join-Path $docs 'CodecSource.zip')
$receipt=@{upstream='FFmpeg 9.0.2';vcpkgBaseline='e456309491fd875487bf02b8a0dca76c1b1b00cc';portTree='b9e158fe3e4f9d727af5e5904e1121d844267df3';sourceArchiveSha256=(Get-FileHash -LiteralPath $sourceArchive).Hash.ToLowerInvariant();correspondingSourceSha256=(Get-FileHash -LiteralPath $zipPath).Hash.ToLowerInvariant();runtime=$runtime;license='LGPL-2.1-or-later';networkRuntime=$false}
[IO.File]::WriteAllText((Join-Path $docs 'BUILD-RECEIPT.json'),($receipt|ConvertTo-Json -Depth 4),[Text.UTF8Encoding]::new($false))
Write-Output 'Prepared exact corresponding source, license and six replaceable codec DLLs. No prebuilt codec archive is used.'
