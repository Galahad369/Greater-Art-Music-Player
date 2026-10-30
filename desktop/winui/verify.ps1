param(
    [Parameter(Mandatory)][string]$Executable,
    [Parameter(Mandatory)][string[]]$Media,
    [Parameter(Mandatory)][string]$OutputDirectory
)
$ErrorActionPreference = 'Stop'
$Executable = (Resolve-Path -LiteralPath $Executable).Path
$bundleHashes = @{}
foreach ($name in @('GreaterArt.WinUI.exe', 'GreaterArt.WinUI.dll', 'GreaterArt.Engine.dll')) {
    $bundleHashes[$name] = (Get-FileHash -LiteralPath (Join-Path (Split-Path $Executable) $name) -Algorithm SHA256).Hash
}
$OutputDirectory = [IO.Path]::GetFullPath($OutputDirectory)
if (Test-Path -LiteralPath $OutputDirectory) { throw 'Use a new evidence directory. Previous evidence is never overwritten.' }
New-Item -ItemType Directory -Path $OutputDirectory | Out-Null
Add-Type -AssemblyName System.Drawing
$results = @()
foreach ($file in $Media) {
    $file = (Resolve-Path -LiteralPath $file).Path
    $caseDirectory = Join-Path $OutputDirectory ('case-' + ($results.Count + 1))
    New-Item -ItemType Directory -Path $caseDirectory | Out-Null
    # The application test mutes playback and does not read or write preferences.
    $probe = Start-Process -FilePath $Executable -ArgumentList @('--verify-flow', ('"' + $file + '"'), ('"' + $caseDirectory + '"')) -WindowStyle Hidden -PassThru
    try {
        if (-not $probe.WaitForExit(90000)) { throw 'The owned playback test exceeded 90 seconds.' }
        $report = Get-Content -LiteralPath (Join-Path $caseDirectory 'flow-result.json') -Raw | ConvertFrom-Json
        $captures = @()
        foreach ($name in @('library', 'expanded', 'mini', 'fullscreen')) {
            $capturePath = Join-Path $caseDirectory ($name + '-video.bmp')
            if (-not (Test-Path -LiteralPath $capturePath)) {
                $captures += [pscustomobject]@{presentation=$name; width=0; height=0; sampledColors=0; nonUniform=$false; error='Capture absent; read the failed flow check.'}
                continue
            }
            $bitmap = [Drawing.Bitmap]::new($capturePath)
            try {
                $colors = [Collections.Generic.HashSet[int]]::new()
                for ($y = 0; $y -lt $bitmap.Height; $y += [Math]::Max(1, [int]($bitmap.Height / 45))) {
                    for ($x = 0; $x -lt $bitmap.Width; $x += [Math]::Max(1, [int]($bitmap.Width / 80))) {
                        [void]$colors.Add($bitmap.GetPixel($x, $y).ToArgb())
                    }
                }
                $captures += [pscustomobject]@{ presentation=$name; width=$bitmap.Width; height=$bitmap.Height; sampledColors=$colors.Count; nonUniform=($colors.Count -gt 16) }
            } finally { $bitmap.Dispose() }
        }
        $passed = $probe.ExitCode -eq 0 -and $report.passed -and @($captures | Where-Object { -not $_.nonUniform }).Count -eq 0
        $results += [pscustomobject]@{ case=$results.Count + 1; exitCode=$probe.ExitCode; passed=$passed; captures=$captures; report='flow-result.json' }
    } finally {
        if (-not $probe.HasExited) { Stop-Process -Id $probe.Id }
        $probe.Dispose()
    }
}
$unchanged = $true
foreach ($name in $bundleHashes.Keys) {
    $unchanged = $unchanged -and $bundleHashes[$name] -eq (Get-FileHash -LiteralPath (Join-Path (Split-Path $Executable) $name) -Algorithm SHA256).Hash
}
$receipt = [pscustomobject]@{
    createdUtc=[DateTime]::UtcNow.ToString('O')
    executableSha256=(Get-FileHash -LiteralPath $Executable -Algorithm SHA256).Hash
    bundleSha256=$bundleHashes
    bundleUnchangedDuringTest=$unchanged
    passed=($unchanged -and @($results | Where-Object { -not $_.passed }).Count -eq 0)
    cases=$results
    limits='Non-uniform owned video captures supplement frame counters. They do not prove acoustic output, clean-machine deployment or monitor refresh rate.'
}
$receipt | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $OutputDirectory 'verification.json') -Encoding UTF8
$receipt | ConvertTo-Json -Depth 8
if (-not $receipt.passed) { throw 'The real playback or visible-image checks failed. Read the preserved evidence.' }
