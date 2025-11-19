$jsonPath = "src/main/resources/inspector_snapshot.json"
if (-not (Test-Path $jsonPath)) {
    Write-Error "Snapshot not found at $jsonPath"
    exit 1
}

function Dedup-List {
    param([object]$List)
    if ($null -eq $List) { return $null }
    $seen = @{}
    $result = @()
    foreach ($item in $List) {
        if ($item -is [array]) {
            $key = ($item | ForEach-Object { $_ }) -join ","
        } elseif ($item -is [pscustomobject] -and $item.PSObject.Properties["row"]) {
            $key = "$($item.row),$($item.col)"
        } else {
            $key = ($item | ForEach-Object { $_ }) -join ","
        }
        if (-not $seen.ContainsKey($key)) {
            $seen[$key] = $true
            $result += ,$item
        }
    }
    return $result
}

$data = Get-Content $jsonPath -Raw | ConvertFrom-Json
$data.solid = Dedup-List $data.solid
$data.water = Dedup-List $data.water
$data.enemyTier1 = Dedup-List $data.enemyTier1
$data.enemyTier2 = Dedup-List $data.enemyTier2
$data.enemyTier3 = Dedup-List $data.enemyTier3
if ($data.doors) {
    foreach ($door in $data.doors) {
        $door.door = Dedup-List $door.door
        $door.key = Dedup-List $door.key
    }
}
$data | ConvertTo-Json -Depth 6 | Set-Content -Encoding UTF8 $jsonPath
Write-Host "Deduped $jsonPath"

