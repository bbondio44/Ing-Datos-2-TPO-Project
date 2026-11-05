<#
  Pruebas de aceptacion de los 40 requisitos contra las bases reales. Guarda la salida como evidencia.
  Uso, desde la raiz del repo, con las cuatro bases cargadas y sin el backend corriendo:
      mvn -B -DskipTests install
      powershell -ExecutionPolicy Bypass -File .\scripts\pruebas\correr-todo.ps1
  Apaga y prende Cassandra, MongoDB y Neo4j para probar las caidas. Tarda unos 8 minutos.
#>
$ErrorActionPreference = 'Continue'
[Console]::OutputEncoding = [Text.Encoding]::UTF8
$raiz = (Get-Location).Path
$temporal = Join-Path $env:TEMP 'pruebas-persona2'
New-Item -ItemType Directory -Force $temporal | Out-Null
cmd /c "mvn -B -q -pl simulator dependency:build-classpath -Dmdep.outputFile=$temporal\sim.cp 2>&1" | Out-Null

# prueba = requisito al que van las secciones que no nombran uno
$pruebas = [ordered]@{ 'r18' = 'R18'; 'fase1' = 'R19'; 'fase2' = 'R09'; 'r40' = 'R40'; 'fase3' = 'R30'; 'roles' = 'R40'; 'persona1' = 'R01'; 'demo-integrada' = 'R17' }
$resumen = @()
foreach ($nombre in $pruebas.Keys) {
    "##### $nombre"
    $salida = powershell -NoProfile -ExecutionPolicy Bypass -File "$PSScriptRoot\$nombre.ps1" -Scratch $temporal 2>&1 | ForEach-Object { "$_" }
    $salida | Where-Object { $_ -match '^(FALLA|=== )' }
    $ok = @($salida | Where-Object { $_ -match '^OK ' }).Count
    $mal = @($salida | Where-Object { $_ -match '^FALLA' }).Count
    $resumen += "$nombre : $ok OK, $mal FALLA"

    $seccion = @(); $titulo = ''
    $volcar = {
        if ($seccion.Count -eq 0) { return }
        $requisitos = @([regex]::Matches($titulo, 'R\d\d') | ForEach-Object { $_.Value } | Select-Object -Unique)
        if ($requisitos.Count -eq 0) { $requisitos = @($pruebas[$nombre]) }
        foreach ($r in $requisitos) {
            $carpeta = Join-Path $raiz "evidencias\$r"
            New-Item -ItemType Directory -Force $carpeta | Out-Null
            $seccion | Out-File -Append -Encoding utf8 (Join-Path $carpeta 'prueba-automatizada.txt')
        }
    }
    if ($nombre -eq 'r18') { Remove-Item "$raiz\evidencias\R*\prueba-automatizada.txt" -ErrorAction SilentlyContinue }
    foreach ($linea in $salida) {
        if ($linea -match '^=== ') { & $volcar; $seccion = @(); $titulo = $linea }
        $seccion += $linea
    }
    & $volcar
}
''
'===== RESUMEN'
$resumen
