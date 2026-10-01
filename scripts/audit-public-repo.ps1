[CmdletBinding()]
param([Parameter(Position = 0)][string]$RepositoryRoot = ".")

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
if (Get-Variable -Name PSNativeCommandUseErrorActionPreference -ErrorAction SilentlyContinue) { $PSNativeCommandUseErrorActionPreference = $false }

$root = (Resolve-Path -LiteralPath $RepositoryRoot).Path
$gitArgs = @("-c", "safe.directory=$($root.Replace('\', '/'))", "-C", $root)
$findings = [System.Collections.Generic.List[string]]::new()

function Invoke-Git { param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments) & git @gitArgs @Arguments }
function Add-Finding {
    param([string]$Message)
    $findings.Add($Message)
    Write-Host "[FAIL] $Message" -ForegroundColor Red
    if ($env:GITHUB_ACTIONS -eq "true") {
        $annotation = $Message.Replace("%", "%25").Replace("`r", "%0D").Replace("`n", "%0A")
        Write-Output "::error title=Public repository audit::$annotation"
    }
}

Invoke-Git rev-parse --is-inside-work-tree | Out-Null
if ($LASTEXITCODE -ne 0) { throw "Not a Git worktree: $root" }

# Split signatures so the scanner does not match its own source.
$credentialPatterns = @(
    ("BEGIN " + "[A-Z ]*PRIVATE KEY"),
    ("github" + "_pat_[A-Za-z0-9_]+"),
    ("gh" + "[pousr]_[A-Za-z0-9]{20,}"),
    ("sk" + "-[A-Za-z0-9_-]{16,}"),
    ("AK" + "IA[0-9A-Z]{16}"),
    ("AI" + "za[A-Za-z0-9_-]{20,}"),
    ("xox" + "[baprs]-[A-Za-z0-9-]+")
)
$credentialPattern = $credentialPatterns -join "|"

# Keep workstation identities out of public source. Split literals so the
# scanner does not match its own implementation.
$localPathPatterns = @(
    ("C:" + "[\\/]+Users[\\/]+[A-Za-z0-9._-]+[\\/]"),
    ("/Users/" + "[A-Za-z0-9._-]+/")
)
$localPathPattern = $localPathPatterns -join "|"

Write-Host "Scanning reachable Git history for credentials and workstation paths..."
$commits = @(Invoke-Git rev-list --all)
foreach ($commit in $commits) {
    $credentialMatches = @(& git @gitArgs grep -I -l -E $credentialPattern $commit -- ":!*.apk" 2>$null)
    if ($LASTEXITCODE -eq 0) {
        foreach ($match in $credentialMatches) { Add-Finding "Credential pattern in reachable history: $match" }
    } elseif ($LASTEXITCODE -gt 1) { throw "git grep failed while scanning credentials in $commit" }

    $pathMatches = @(& git @gitArgs grep -I -l -E $localPathPattern $commit -- ":!*.apk" 2>$null)
    if ($LASTEXITCODE -eq 0) {
        foreach ($match in $pathMatches) { Add-Finding "Workstation home path in reachable history: $match" }
    } elseif ($LASTEXITCODE -gt 1) { throw "git grep failed while scanning workstation paths in $commit" }
}

Write-Host "Scanning current working files for credentials..."
$workingFiles = @(Invoke-Git ls-files --cached --others --exclude-standard)
foreach ($relativePath in $workingFiles) {
    try {
        $path = Join-Path $root $relativePath
        if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { continue }
        $item = Get-Item -LiteralPath $path
        if ($item.Length -gt 5MB -or $item.Extension -eq ".apk") { continue }
        $content = [System.IO.File]::ReadAllText($path)
        if ($content -match $credentialPattern) { Add-Finding "Credential pattern in working file: $relativePath" }
        if ($content -match $localPathPattern) { Add-Finding "Workstation home path in working file: $relativePath" }
    } catch [System.IO.IOException] { continue } catch [System.UnauthorizedAccessException] { continue }
}

Write-Host "Scanning history for sensitive filenames..."
$sensitiveNamePattern = "(^|/)(\.env($|\.)|local\.properties$|google-services\.json$|.*\.(jks|keystore|p12|pfx|pem|key)$|credentials?.*\.json$|secrets?\.(properties|json|ya?ml)$|LINKEDIN_POST\.md$)"
$objectPaths = @(Invoke-Git rev-list --objects --all | ForEach-Object { $parts = $_ -split " ", 2; if ($parts.Count -eq 2) { $parts[1] } })
foreach ($path in $objectPaths) { if ($path -and $path -match $sensitiveNamePattern) { Add-Finding "Sensitive filename in reachable history: $path" } }

Write-Host "Checking public commit identities..."
$emails = @(Invoke-Git log --all --format=%ae | Sort-Object -Unique)
foreach ($email in $emails) {
    if ($email -and $email -notmatch "@users\.noreply\.github\.com$" -and $email -notin @("noreply@github.com", "copilot@github.com")) {
        Add-Finding "Commit author email is not a GitHub noreply address"
    }
}

Write-Host "Checking APK containers for sensitive entries..."
if (-not ("System.IO.Compression.ZipFile" -as [type])) { Add-Type -AssemblyName System.IO.Compression.FileSystem }
$apks = @(Invoke-Git ls-files "*.apk")
foreach ($relativePath in $apks) {
    # A local artifact rename can leave old tracked names pending deletion.
    # Audit the containers that actually exist without altering the user's index.
    if (-not (Test-Path -LiteralPath (Join-Path $root $relativePath) -PathType Leaf)) { continue }
    $archive = [System.IO.Compression.ZipFile]::OpenRead((Join-Path $root $relativePath))
    try {
        foreach ($entry in $archive.Entries) {
            if ($entry.FullName -match $sensitiveNamePattern) { Add-Finding "Sensitive entry inside ${relativePath}: $($entry.FullName)" }
        }
    } finally { $archive.Dispose() }
}

if ($findings.Count -gt 0) {
    Write-Host "Public repository audit failed with $($findings.Count) finding(s)." -ForegroundColor Red
    exit 1
}
Write-Host "[OK] No high-confidence secrets, workstation home paths, sensitive filenames, or public author emails found." -ForegroundColor Green
