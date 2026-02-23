param(
    [string]$KeystorePath = "keystore/upload-keystore.jks",
    [string]$Alias = "upload",
    [int]$ValidityDays = 10950,
    [int]$KeySize = 4096
)

$keytoolCmd = Get-Command keytool -ErrorAction SilentlyContinue
if (-not $keytoolCmd) {
    Write-Error "keytool nicht gefunden. Bitte JDK installieren oder keytool in PATH setzen."
    exit 1
}

$targetDir = Split-Path -Parent $KeystorePath
if ($targetDir) {
    New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
}

& $keytoolCmd.Source -genkeypair -v `
    -keystore $KeystorePath `
    -alias $Alias `
    -keyalg RSA `
    -keysize $KeySize `
    -validity $ValidityDays

if ($LASTEXITCODE -ne 0) {
    Write-Error "Keystore-Erstellung fehlgeschlagen."
    exit $LASTEXITCODE
}

Write-Output "Keystore erstellt: $KeystorePath"
Write-Output "Naechster Schritt: keystore.properties mit deinen Daten anlegen."
