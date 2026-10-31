param([Parameter(Mandatory)][string]$Artifact)
$ErrorActionPreference = 'Stop'
$item = Get-Item -LiteralPath $Artifact
$signature = Get-AuthenticodeSignature -LiteralPath $item.FullName
$fingerprint = $null
if ($signature.SignerCertificate) {
    $sha = [Security.Cryptography.SHA256]::Create()
    try { $fingerprint = ([BitConverter]::ToString($sha.ComputeHash($signature.SignerCertificate.RawData))).Replace('-', '').ToLowerInvariant() }
    finally { $sha.Dispose() }
}
[pscustomobject]@{
    status = $signature.Status.ToString()
    certificateSha256 = $fingerprint
    version = $item.VersionInfo.FileVersion
} | ConvertTo-Json -Compress
