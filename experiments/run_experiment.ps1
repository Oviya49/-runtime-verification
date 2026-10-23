param(
    [string]$Detector = "VERIFIER",
    [string[]]$Faults = @("NONE", "F1_MISSING_CONTEXT", "F2_STALE_REUSE", "F3_ASYNC_LOSS", "F4_MISSING_FILTER"),
    [int]$Messages = 100,
    [int]$Seed = 1,
    [string]$RunId = (Get-Date -Format "yyyyMMdd-HHmmss")
)

$root = "C:\Projects\tenantguard-project"
$out = "$root\experiments\runs\$RunId"
New-Item -ItemType Directory -Force -Path $out | Out-Null
Set-Location "$root\docker"
$env:TENANTGUARD_DETECTOR_MODE = $Detector
$rng = New-Object System.Random $Seed

$ledger = @()
foreach ($fault in $Faults) {
    $env:TENANTGUARD_FAULT_MODE = $fault
    docker compose up -d --force-recreate service-b | Out-Null
    Start-Sleep -Seconds 15

    for ($i = 1; $i -le $Messages; $i++) {
        $tenant = "tenant-" + ($rng.Next(1, 4))
        $body = @{ tenantId = $tenant; action = "CREATE"; payload = "hello" } | ConvertTo-Json
        $resp = [string](Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/request/send -ContentType "application/json" -Body $body)
        $match = [regex]::Match($resp, 'RequestID: ([0-9a-fA-F-]{36})')
        if (-not $match.Success) { throw "No RequestID in response: $resp" }
        $ledger += [pscustomobject]@{
            runId = $RunId; seed = $Seed; detector = $Detector; expectedFault = $fault
            requestId = $match.Groups[1].Value; tenantId = $tenant; sentAt = (Get-Date).ToString("o")
        }
        Start-Sleep -Milliseconds $rng.Next(0, 30)
    }

    Start-Sleep -Seconds 3
    $m = Invoke-RestMethod -Uri http://localhost:8082/api/metrics/all
    $m.metrics | ForEach-Object {
        [pscustomobject]@{
            runId = $RunId; seed = $Seed; detector = $Detector; requestId = $_.requestId
            observedFault = $_.faultType; detected = $_.detected; detectionTimeMs = $_.detectionTimeMs
        }
    } | Export-Csv "$out\observed_$fault.csv" -NoTypeInformation
}

$ledger | Export-Csv "$out\ledger.csv" -NoTypeInformation
$env:TENANTGUARD_FAULT_MODE = "NONE"
Write-Host "Run $RunId saved to $out"