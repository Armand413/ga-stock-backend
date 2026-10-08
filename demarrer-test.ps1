$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

# Saisie locale uniquement : ne pas enregistrer le secret dans le projet.
$ancienMotDePasse = $env:DB_PASSWORD
$codeSortie = 1
try {
    if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) {
        $secret = Read-Host 'Mot de passe PostgreSQL (utilisateur postgres par defaut)' -AsSecureString
        $pointeur = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secret)
        try {
            $env:DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointeur)
        } finally {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointeur)
            $secret.Dispose()
        }
    }
    if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) {
        throw 'Le mot de passe PostgreSQL est obligatoire.'
    }
    & "$PSScriptRoot\mvnw.cmd" spring-boot:run '-Dspring-boot.run.profiles=local-test'
    $codeSortie = $LASTEXITCODE
} finally {
    $env:DB_PASSWORD = $ancienMotDePasse
}
exit $codeSortie
