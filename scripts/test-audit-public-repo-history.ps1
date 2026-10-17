# Issue #24: deleted history must be detected for Windows, macOS and Linux paths.
[CmdletBinding()]
param()
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
if (Get-Variable PSNativeCommandUseErrorActionPreference -ErrorAction SilentlyContinue) { $PSNativeCommandUseErrorActionPreference = $false }
$audit = Join-Path $PSScriptRoot "audit-public-repo.ps1"
$repo = Join-Path ([IO.Path]::GetTempPath()) ("ga-audit-" + [guid]::NewGuid().ToString("N"))
function Invoke-TestGit { param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments) & git -C $repo @Arguments | Out-Null; if ($LASTEXITCODE -ne 0) { throw "Synthetic git operation failed" } }
function Check { $out = (& pwsh -NoProfile -NonInteractive -File $audit $repo 2>&1 | Out-String); return [pscustomobject]@{ code = $LASTEXITCODE; output = $out } }
try {
    New-Item -ItemType Directory $repo | Out-Null
    Invoke-TestGit init -q
    Invoke-TestGit config user.name "Synthetic Test"
    Invoke-TestGit config user.email "123456+privacy-test@users.noreply.github.com"
    Set-Content (Join-Path $repo "README.txt") "clean test"
    Invoke-TestGit add -- README.txt
    Invoke-TestGit commit -q -m "clean"
    if ((Check).code -ne 0) { throw "Clean history did not pass" }

    $slash = [char]92
    $fixtures = @{
        "win-space.log" = "Z:" + $slash + "Users" + $slash + "Synthetic Person" + $slash + "Documents" + $slash
        "win-lower.log" = "c:/" + "users/" + "synthetic-person/Documents/"
        "mac-space.log" = "/" + "Users" + "/Synthetic Person/Documents/"
        "linux-space.log" = "/" + "home" + "/synthetic-person/Documents/"
    }
    foreach ($name in $fixtures.Keys) {
        Set-Content (Join-Path $repo $name) ("Test log: " + $fixtures[$name])
        Invoke-TestGit add -- $name
    }
    Invoke-TestGit commit -q -m "historical synthetic paths"
    foreach ($name in $fixtures.Keys) { Invoke-TestGit rm -q -- $name }
    Invoke-TestGit commit -q -m "remove synthetic logs"

    $result = Check
    if ($result.code -eq 0) { throw "Deleted historical workstation paths escaped detection" }
    foreach ($name in $fixtures.Keys) {
        if ($result.output -notmatch [regex]::Escape("Workstation home path in reachable history: " + $name)) {
            throw "Historical path not detected for $name"
        }
    }
    foreach ($identity in @("Synthetic Person", "synthetic-person")) {
        if ($result.output.Contains($identity)) { throw "Audit printed a workstation identity" }
    }
    Write-Host "[OK] Four deleted historical path variants detected without identity disclosure."
} finally {
    if (Test-Path $repo) { Remove-Item -LiteralPath $repo -Recurse -Force }
}
# Audit subprocess failure is expected for synthetic findings.
exit 0
