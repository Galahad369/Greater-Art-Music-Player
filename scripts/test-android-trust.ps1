param(
    [Parameter(Mandatory)][string]$AdbPath,
    [Parameter(Mandatory)][ValidatePattern('^emulator-[0-9]+$')][string]$Serial,
    [Parameter(Mandatory)][string]$EvidenceDirectory,
    [Parameter(Mandatory)][ValidatePattern('^[0-9]+\.[0-9]+\.[0-9]+$')][string]$ExpectedVersion,
    [Parameter(Mandatory)][int]$ExpectedCode
)
$ErrorActionPreference = 'Stop'
$package = 'com.local.listentomusic'
$runner = "$package.test/androidx.test.runner.AndroidJUnitRunner"

function Invoke-QaAdb([string[]]$Arguments) {
    $output = & $AdbPath -s $Serial @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) { throw 'QA ADB command failed; no acceptance result is recorded.' }
    return $output -join "`n"
}

if (-not (Test-Path -LiteralPath $AdbPath -PathType Leaf)) { throw 'ADB executable is missing.' }
$avd = Invoke-QaAdb @('emu', 'avd', 'name')
if (($avd -split '\s+') -notcontains 'GreaterArt_Scroll_QA36') { throw 'Use only the owned Greater Art QA emulator.' }
$installed = Invoke-QaAdb @('shell', 'dumpsys', 'package', $package)
if ($installed -notmatch "versionCode=$ExpectedCode\b" -or
    $installed -notmatch ('versionName=' + [regex]::Escape($ExpectedVersion) + '(\s|$)')) {
    throw 'Installed application identity does not match the requested test build.'
}
New-Item -ItemType Directory -Force -Path $EvidenceDirectory | Out-Null
foreach ($label in @('trust-denied', 'trust-granted', 'trust-revoked')) {
    if (Test-Path -LiteralPath (Join-Path $EvidenceDirectory "$label.log")) {
        throw 'Use a fresh evidence directory; test logs are never overwritten.'
    }
}
$operations = @('MANAGE_EXTERNAL_STORAGE', 'SYSTEM_ALERT_WINDOW')
$previous = @{}
foreach ($operation in $operations) {
    $mode = Invoke-QaAdb @('shell', 'appops', 'get', $package, $operation)
    if ($mode -match "$operation`: (allow|deny|ignore|default|foreground)") { $previous[$operation] = $Matches[1] }
    elseif ($mode -match 'No operations') { $previous[$operation] = 'default' }
    else { throw 'Could not read the original permission state; refusing to change it.' }
}

function Test-Permissions([string]$Label, [string]$Granted, [string]$Classes, [int]$Count) {
    $arguments = @('shell', 'am', 'instrument', '-w', '-r', '-e', 'class', $Classes,
        '-e', 'expectedStorageGranted', $Granted, '-e', 'expectedOverlayGranted', $Granted,
        '-e', 'permissionReturnFixture', 'true', $runner)
    $output = Invoke-QaAdb $arguments
    $output | Set-Content -LiteralPath (Join-Path $EvidenceDirectory "$Label.log") -Encoding utf8
    if ($output -match 'FAILURES|INSTRUMENTATION_FAILED|shortMsg=|INSTRUMENTATION_STATUS_CODE: -[23]' -or
        $output -notmatch "OK \($Count tests?\)") {
        throw "Permission suite $Label did not pass; inspect its retained log."
    }
    Write-Output "$Label`: $Count passed"
}

# Android kills a UID when All Files Access is revoked. Never revoke it from
# instrumentation running inside that UID. The host restores state even on failure.
try {
    foreach ($operation in $operations) { $null = Invoke-QaAdb @('shell', 'appops', 'set', $package, $operation, 'deny') }
    Test-Permissions 'trust-denied' 'false' "$package.diagnostics.AppTrustSnapshotDeviceTest,$package.ui.TrustPrivacyUiTest" 7
    foreach ($operation in $operations) { $null = Invoke-QaAdb @('shell', 'appops', 'set', $package, $operation, 'allow') }
    Test-Permissions 'trust-granted' 'true' "$package.diagnostics.AppTrustSnapshotDeviceTest#permissionsMatchTheExternallyConfiguredState" 1
    foreach ($operation in $operations) { $null = Invoke-QaAdb @('shell', 'appops', 'set', $package, $operation, 'deny') }
    Test-Permissions 'trust-revoked' 'false' "$package.diagnostics.AppTrustSnapshotDeviceTest#permissionsMatchTheExternallyConfiguredState" 1
} finally {
    foreach ($operation in $operations) {
        $null = Invoke-QaAdb @('shell', 'appops', 'set', $package, $operation, $previous[$operation])
    }
}
Write-Output 'All nine trust cases passed; original grants restored.'
