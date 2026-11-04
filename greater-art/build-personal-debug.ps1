[CmdletBinding()]
param(
    [string[]]$Tasks = @('testDebugUnitTest', 'lintDebug', 'assembleDebug'),
    [switch]$UseWindowsTrustedRoots,
    [switch]$BuildOnly
)
$ErrorActionPreference = "Stop"

$projectRoot = $PSScriptRoot
$profileRoot = $env:USERPROFILE
if ([string]::IsNullOrWhiteSpace($profileRoot)) {
    throw "USERPROFILE is unavailable; refusing to create an APK with an unknown signing identity."
}

$debugKeystore = Join-Path $profileRoot ".android\debug.keystore"
if (-not (Test-Path -LiteralPath $debugKeystore)) {
    throw "The established Android debug keystore was not found at $debugKeystore."
}

$previousJavaHome = $env:JAVA_HOME
$selectedJavaHome = $previousJavaHome
if ([string]::IsNullOrWhiteSpace($selectedJavaHome) -or
    -not (Test-Path -LiteralPath (Join-Path $selectedJavaHome "bin\java.exe"))) {
    $jdk = Get-ChildItem "${env:ProgramFiles}\Android\openjdk" -Directory -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending |
        Select-Object -First 1
    if ($null -eq $jdk) {
        throw "Set JAVA_HOME to Android Studio's JDK 21, then run this script again."
    }
    $selectedJavaHome = $jdk.FullName
}

$previousAndroidHome = $env:ANDROID_HOME
$previousAndroidSdkRoot = $env:ANDROID_SDK_ROOT
$previousJavaToolOptions = $env:JAVA_TOOL_OPTIONS
$androidSdk = $env:ANDROID_HOME
if ([string]::IsNullOrWhiteSpace($androidSdk) -or
    -not (Test-Path -LiteralPath (Join-Path $androidSdk "platforms\android-37.0\android.jar"))) {
    $androidSdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
}
if (-not (Test-Path -LiteralPath (Join-Path $androidSdk "platforms\android-37.0\android.jar"))) {
    throw "Android SDK platform 37 was not found. Install it with Android Studio's SDK Manager."
}
Push-Location $projectRoot
try {
    $env:JAVA_HOME = $selectedJavaHome
    $env:ANDROID_HOME = $androidSdk
    $env:ANDROID_SDK_ROOT = $androidSdk
    $java = Join-Path $env:JAVA_HOME "bin\java.exe"
    if ($UseWindowsTrustedRoots) {
        if ($previousJavaToolOptions -match 'javax\.net\.ssl\.trustStore') {
            throw 'Existing Java trust-store options must be resolved explicitly, not overwritten.'
        }
        # Inherit into the wrapper, Gradle daemon and compiler children. Read-only
        # native ROOT store; never Windows-MY, certificate import or a trust-all manager.
        $env:JAVA_TOOL_OPTIONS = ($previousJavaToolOptions +
            ' -Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NONE').Trim()
    }
    $wrapperJar = Join-Path $projectRoot "gradle\wrapper\gradle-wrapper.jar"
    & $java "-Duser.home=$profileRoot" "-Dorg.gradle.appname=gradlew" `
        -classpath $wrapperJar org.gradle.wrapper.GradleWrapperMain @Tasks `
        --no-daemon --no-parallel --max-workers=1 `
        '-Dorg.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8' `
        '-Pkotlin.compiler.execution.strategy=in-process'
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle verification failed with exit code $LASTEXITCODE."
    }
    if ($BuildOnly) { return }
    if ($Tasks -notcontains 'assembleDebug' -and $Tasks -notcontains ':app:assembleDebug') {
        throw 'Candidate copying requires an explicit assembleDebug task; use -BuildOnly for other tasks.'
    }

    $metadataPath = Join-Path $projectRoot "app\build\outputs\apk\debug\output-metadata.json"
    $metadata = Get-Content -Raw -LiteralPath $metadataPath | ConvertFrom-Json
    $sourceApk = Join-Path $projectRoot "app\build\outputs\apk\debug\$($metadata.elements[0].outputFile)"
    # Compilation is not release acceptance. Never publish a SOURCE_ONLY debug
    # build into releases/ or overwrite an existing versioned candidate.
    $candidateRoot = Join-Path $projectRoot 'build/candidates'
    New-Item -ItemType Directory -Path $candidateRoot -Force | Out-Null
    $destination = Join-Path $candidateRoot "GreaterArt-$($metadata.elements[0].versionName)-candidate.apk"
    if (Test-Path -LiteralPath $destination) {
        throw "Versioned APK already exists: $destination. Increase the version; never overwrite a release."
    }
    $apkSigner = Join-Path $androidSdk "build-tools/37.0.0/apksigner.bat"
    $signature = & $apkSigner verify --print-certs $sourceApk
    if ($LASTEXITCODE -ne 0 -or ($signature -join "`n") -notmatch "9e28eb45b3b171c3ea47d7da942d28d88b16538885e392a6971a80906d612fbf") {
        throw "APK signing identity does not match the pinned sideload certificate. Nothing was published."
    }
    $partial = $destination + '.' + [Guid]::NewGuid().ToString('N') + '.partial'
    [System.IO.File]::Copy($sourceApk, $partial, $false)
    if ((Get-FileHash -LiteralPath $sourceApk -Algorithm SHA256).Hash -ne
            (Get-FileHash -LiteralPath $partial -Algorithm SHA256).Hash) {
        throw 'Candidate copy mismatch; partial retained and not published.'
    }
    [System.IO.File]::Move($partial, $destination)
    Write-Output "Built candidate (not a verified release): $destination"
} finally {
    $env:JAVA_HOME = $previousJavaHome
    $env:ANDROID_HOME = $previousAndroidHome
    $env:ANDROID_SDK_ROOT = $previousAndroidSdkRoot
    $env:JAVA_TOOL_OPTIONS = $previousJavaToolOptions
    Pop-Location
}
