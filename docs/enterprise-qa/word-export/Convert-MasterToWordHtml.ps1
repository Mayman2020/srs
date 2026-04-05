# Regenerates QA_Master_ForWord.html from QA_Master_Delivery_Package_AR_EN.md
# Run from repo: .\Convert-MasterToWordHtml.ps1
$ErrorActionPreference = "Stop"
$here = $PSScriptRoot
$mdPath = Join-Path $here "..\QA_Master_Delivery_Package_AR_EN.md"
$outPath = Join-Path $here "QA_Master_ForWord.html"
Add-Type -AssemblyName System.Web
$lines = Get-Content -LiteralPath $mdPath -Encoding UTF8
$sb = New-Object System.Text.StringBuilder

function Escape-Html([string]$s) {
  if ($null -eq $s) { return "" }
  [System.Web.HttpUtility]::HtmlEncode($s)
}

function Is-TableSep([string]$line) {
  $s = $line.Trim()
  if (-not $s.StartsWith('|')) { return $false }
  # GFM separator rows: only pipes, dashes, colons, spaces — no letters (Latin / Arabic)
  if ($s -match '[A-Za-z\u0600-\u06FF]') { return $false }
  return ($s -match '\-|:')
}

function Row-ToCells([string]$line) {
  $parts = $line.TrimEnd() -split '\|'
  $cells = New-Object System.Collections.Generic.List[string]
  for ($i = 1; $i -lt $parts.Length - 1; $i++) {
    $cells.Add($parts[$i].Trim())
  }
  return $cells
}

function NextNonEmptyTableLineIsSep([string[]]$all, [int]$idx) {
  for ($j = $idx + 1; $j -lt $all.Length; $j++) {
    $nx = $all[$j].Trim()
    if ($nx -eq '') { continue }
    return (Is-TableSep $nx)
  }
  return $false
}

[void]$sb.AppendLine('<!DOCTYPE html>')
[void]$sb.AppendLine('<html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:w="urn:schemas-microsoft-com:office:word">')
[void]$sb.AppendLine('<head>')
[void]$sb.AppendLine('<meta charset="utf-8" />')
[void]$sb.AppendLine('<meta name="ProgId" content="Word.Document" />')
[void]$sb.AppendLine('<title>Master QA Delivery Package - Administrative Communications System</title>')
[void]$sb.AppendLine('<style type="text/css">')
[void]$sb.AppendLine('body { font-family: "Segoe UI", "Traditional Arabic", Arial, sans-serif; font-size: 11pt; line-height: 1.35; margin: 2.5cm; color: #1a1a1a; }')
[void]$sb.AppendLine('h1 { font-size: 22pt; text-align: center; margin-top: 0.5em; }')
[void]$sb.AppendLine('h2 { font-size: 16pt; margin-top: 1.4em; page-break-after: avoid; border-bottom: 1pt solid #333; padding-bottom: 4px; }')
[void]$sb.AppendLine('h3 { font-size: 13pt; margin-top: 1em; page-break-after: avoid; }')
[void]$sb.AppendLine('h4 { font-size: 11pt; margin-top: 0.8em; font-weight: bold; }')
[void]$sb.AppendLine('table { border-collapse: collapse; width: 100%; margin: 10px 0 16px 0; font-size: 8.5pt; }')
[void]$sb.AppendLine('th, td { border: 1px solid #444; padding: 4px 5px; vertical-align: top; }')
[void]$sb.AppendLine('th { background: #e8eef5; font-weight: bold; }')
[void]$sb.AppendLine('.cover { text-align: center; margin: 2.5cm 1.5cm; }')
[void]$sb.AppendLine('.cover-box { border: 2px solid #1a365d; padding: 28px; margin: 20px auto; max-width: 560px; }')
[void]$sb.AppendLine('.pagebreak { page-break-before: always; }')
[void]$sb.AppendLine('p { margin: 0.45em 0; }')
[void]$sb.AppendLine('ul { margin: 0.3em 0 0.6em 1.2em; }')
[void]$sb.AppendLine('hr { border: none; border-top: 1px solid #ccc; margin: 20px 0; }')
[void]$sb.AppendLine('code { font-family: Consolas, monospace; font-size: 8.5pt; background: #f5f5f5; padding: 1px 3px; }')
[void]$sb.AppendLine('</style></head><body>')

$inTable = $false
$tableRowIndex = 0
$tableHasGfmHeader = $false
$coverOpen = $false

for ($lineNum = 0; $lineNum -lt $lines.Length; $lineNum++) {
  $raw = $lines[$lineNum]
  $line = $raw.TrimEnd()
  $t = $line.Trim()

  if ($t.StartsWith('<div align="center">')) {
    [void]$sb.AppendLine('<div class="cover"><div class="cover-box">')
    $coverOpen = $true
    continue
  }
  if ($coverOpen -and $t -eq '</div>') {
    [void]$sb.AppendLine('</div></div>')
    $coverOpen = $false
    continue
  }

  if ($t -match '^\|' -and -not (Is-TableSep $t)) {
    if (-not $inTable) {
      [void]$sb.AppendLine('<table>')
      $inTable = $true
      $tableRowIndex = 0
      $tableHasGfmHeader = (NextNonEmptyTableLineIsSep $lines $lineNum)
    }
    $cells = Row-ToCells $line
    [void]$sb.AppendLine('<tr>')
    $useTh = $tableHasGfmHeader -and ($tableRowIndex -eq 0)
    $tableRowIndex++
    foreach ($c in $cells) {
      $inner = (Escape-Html $c) -replace '\*\*([^*]+)\*\*', '<strong>$1</strong>'
      $inner = $inner -replace '`([^`]+)`', '<code>$1</code>'
      $tag = if ($useTh) { "th" } else { "td" }
      [void]$sb.AppendLine("<$tag>$inner</$tag>")
    }
    [void]$sb.AppendLine('</tr>')
    continue
  }

  if ($inTable -and (Is-TableSep $t)) {
    continue
  }

  if ($inTable -and -not ($t -match '^\|')) {
    [void]$sb.AppendLine('</table>')
    $inTable = $false
    $tableRowIndex = 0
    $tableHasGfmHeader = $false
  }

  if ($t -match '^\|') { continue }

  if ($t -eq '---') {
    [void]$sb.AppendLine('<hr />')
    continue
  }
  if ($t -match '^<span id=') {
    [void]$sb.AppendLine($t)
    continue
  }
  if ($t -match '^<div style="page-break') {
    [void]$sb.AppendLine('<p class="pagebreak"></p>')
    continue
  }
  if ($t -match '^####\s+(.+)$') {
    [void]$sb.AppendLine('<h4>' + (Escape-Html $Matches[1]) + '</h4>')
    continue
  }
  if ($t -match '^###\s+(.+)$') {
    [void]$sb.AppendLine('<h3>' + (Escape-Html $Matches[1]) + '</h3>')
    continue
  }
  if ($t -match '^##\s+(.+)$') {
    [void]$sb.AppendLine('<h2>' + (Escape-Html $Matches[1]) + '</h2>')
    continue
  }
  if ($t -match '^#\s+(.+)$') {
    [void]$sb.AppendLine('<h1 style="margin-top:0.2em;color:#1a365d">' + (Escape-Html $Matches[1].Trim()) + '</h1>')
    continue
  }
  if ($t -eq '<br/>' -or $t -eq '<br>') { continue }

  if ([string]::IsNullOrWhiteSpace($t)) { continue }

  if ($t.StartsWith('- ')) {
    $li = Escape-Html $t.Substring(2)
    $li = $li -replace '\*\*([^*]+)\*\*', '<strong>$1</strong>'
    [void]$sb.AppendLine("<ul><li>$li</li></ul>")
    continue
  }

  $para = Escape-Html $t
  $para = $para -replace '\*\*([^*]+)\*\*', '<strong>$1</strong>'
  $para = $para -replace '`([^`]+)`', '<code>$1</code>'
  if ($t.StartsWith('*') -and $t.EndsWith('*') -and $t.Length -gt 2 -and -not $t.StartsWith('**')) {
    [void]$sb.AppendLine('<p><em>' + (Escape-Html $t.Trim('*')) + '</em></p>')
    continue
  }
  [void]$sb.AppendLine("<p>$para</p>")
}

if ($inTable) { [void]$sb.AppendLine('</table>') }

[void]$sb.AppendLine('</body></html>')
$utf8bom = New-Object System.Text.UTF8Encoding $true
[System.IO.File]::WriteAllText($outPath, $sb.ToString(), $utf8bom)
Write-Host "OK:" $outPath "size:" (Get-Item $outPath).Length
