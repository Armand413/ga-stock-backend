param([Parameter(Mandatory=$true)][string]$MotDePasse, [string]$Identifiant='admin.ga')
$ErrorActionPreference='Stop'
$base='http://127.0.0.1:8080/api'
$rows=Import-Csv (Join-Path $PSScriptRoot 'consommables-factures-2026-07-02.csv') -Delimiter ';'
$session=Invoke-RestMethod "$base/auth/login" -Method Post -ContentType 'application/json' -Body (@{identifiant=$Identifiant;motDePasse=$MotDePasse}|ConvertTo-Json)
$headers=@{Authorization="Bearer $($session.accessToken)"}
$journal=[System.Collections.Generic.List[object]]::new()
function Send-Json($url,$data) { Invoke-RestMethod $url -Method Post -Headers $headers -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes(($data|ConvertTo-Json))) }
try {
 foreach($row in $rows) {
  $unit=$row.unite; $quantity=0L
  if($row.quantiteFacturee -eq '1/2') { $unit='demi-carton'; $quantity=1 } else { $quantity=[long]$row.quantiteFacturee }
  $found=Invoke-RestMethod "$base/articles?size=100&recherche=$([uri]::EscapeDataString($row.reference))" -Headers $headers
  $article=@($found.content | Where-Object reference -eq $row.reference)
  if($article.Count -gt 1){throw "Reference ambigue : $($row.reference)"}
  if($article.Count -eq 0){$a=Send-Json "$base/articles" @{reference=$row.reference;nom=$row.nom;unite=$unit;seuilAlerte=0}} else {$a=$article[0];if($a.nom -ne $row.nom -or $a.unite -ne $unit){throw "Article existant incompatible : $($row.reference)"}}
  $marker="Import facture $($row.facture) du 02/07/2026 - $($row.reference)"
  $page=0; $existing=@()
  do { $history=Invoke-RestMethod "$base/mouvements?articleId=$($a.id)&size=100&page=$page" -Headers $headers; $existing+=@($history.content | Where-Object motif -eq $marker); $page++ } while($page -lt $history.totalPages)
  if($existing.Count -gt 0){if($existing.Count -ne 1 -or $existing[0].quantite -ne $quantity -or $existing[0].type -ne 'ENTREE'){throw "Mouvement incompatible : $($row.reference)"};$movement=$existing[0];$state='Déjà importé'} else {$movement=Send-Json "$base/articles/$($a.id)/mouvements" @{type='ENTREE';quantite=$quantity;motif=$marker};$state='Importé'}
  $check=Invoke-RestMethod "$base/articles/$($a.id)" -Headers $headers
  $journal.Add([pscustomobject]@{reference=$row.reference;nom=$row.nom;articleId=$a.id;unite=$unit;quantiteImportee=$quantity;stockActuel=$check.quantite;mouvementId=$movement.id;facture=$row.facture;etat=$state})
  $journal | Export-Csv (Join-Path $PSScriptRoot 'resultat-import-2026-07-02.csv') -Delimiter ';' -NoTypeInformation -Encoding utf8
 }
 "Articles vérifiés : $($journal.Count)"
 $journal | Group-Object etat | Select-Object Name,Count
} finally { Invoke-RestMethod "$base/auth/logout" -Method Post -Headers $headers | Out-Null }
