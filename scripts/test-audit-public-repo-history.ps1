# Issue #24 synthetic regression: a deleted log must still fail the history audit.
[CmdletBinding()]
param()
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
if (Get-Variable PSNativeCommandUseErrorActionPreference -ErrorAction SilentlyContinue) { $PSNativeCommandUseErrorActionPreference = $false }
$audit = Join-Path $PSScriptRoot "audit-public-repo.ps1"
$repo = Join-Path ([IO.Path]::GetTempPath()) ("ga-audit-" + [guid]::NewGuid().ToString("N"))
function Git { param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Args) & git -C $repo @Args | Out-Null; if ($LASTEXITCODE -ne 0) { throw "Synthetic git operation failed" } }
function Check { $out = (& pwsh -NoProfile -NonInteractive -File $audit $repo 2>&1 | Out-String); return [pscustomobject]@{ code = $LASTEXITCODE; output = $out } }
try {
    New-Item -ItemType Directory $repo | Out-Null
    Git init -q
    Git config user.name "Synthetic Test"
    Git config user.email "123456+privacy-test@users.noreply.github.com"
    Set-Content (Join-Path $repo "README.txt") "clean test"
    Git add -- README.txt
    Git commit -q -m "clean"
    if ((Check).code -ne 0) { throw "Clean history did not pass" }
    # Construct only at runtime to avoid matching this regression's source.
    $fake = "C:" + [char]92 + "Users" + [char]92 + "synthetic-person" + [char]92 + "Documents" + [char]92
    Set-Content (Join-Path $repo "old-build.log") ("Test log: " + $fake)
    Git add -- old-build.log
    Git commit -q -m "historical test"
    Git rm -q -- old-build.log
    Git commit -q -m "remove log"
    if (Test-Path (Join-Path $repo "old-build.log")) { throw "Log was not removed at HEAD" }
    $historical = Check
    if ($historical.code -eq 0 -or $historical.output -notmatch "Workstation home path in reachable history: old-build.log") {
        throw "Deleted historical workstation path was not detected"
    }
    if ($historical.output.Contains("synthetic-person")) { throw "Audit printed a workstation identity" }
    Write-Host "[OK] Historical deleted-file exposure detected; value is not echoed."
} finally {
    if (Test-Path $repo) { Remove-Item -LiteralPath $repo -Recurse -Force }
}
