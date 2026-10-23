param([string]$Detector = "VERIFIER", [int]$Messages = 100, [int]$Seed = 1, [string]$RunId)

$root = "C:\Projects\tenantguard-project"
$out = "$root\experiments\runs\$RunId"
New-Item -ItemType Directory -Force -Path $out | Out-Null
Set-Location "$root\docker"
$env:TENANTGUARD_DETECTOR_MODE = $Detector
$env:TENANTGUARD_FAULT_MODE = "NONE"
$rng = New-Object System.Random $Seed
$hmac = New-Object System.Security.Cryptography.HMACSHA256 (,[Text.Encoding]::UTF8.GetBytes("dev-only-key"))
$cred = New-Object System.Management.Automation.PSCredential("guest", (ConvertTo-SecureString "guest" -AsPlainText -Force))

function Sign([string]$t, [string]$r, [string]$p) {
    [Convert]::ToBase64String($hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes("$t|$r|$p")))
}

function Publish([string]$headerTenant, [string]$bodyTenant, [string]$reqId, [string]$producedAt, [string]$sig) {
    $payload = @{ tenantId = $bodyTenant; requestId = $reqId; action = "CREATE"; payload = "hello" } | ConvertTo-Json -Compress
    $msg = @{
        properties = @{ headers = @{ "X-Tenant-ID" = $headerTenant; "X-Produced-At" = $producedAt; "X-Auth-Sig" = $sig } }
        routing_key = "tenant-requests"; payload = $payload; payload_encoding = "string"
    } | ConvertTo-Json -Depth 5
    Invoke-RestMethod -Method Post -Uri "http://localhost:15672/api/exchanges/%2F/amq.default/publish" -Credential $cred -ContentType "application/json" -Body $msg
}

$ledger = @()
foreach ($cond in @("H1_FORGED_SIG", "H2_BODY_MISMATCH")) {
    docker compose up -d --force-recreate service-b | Out-Null
    Start-Sleep -Seconds 15

    for ($i = 1; $i -le $Messages; $i++) {
        $reqId = "$RunId-$cond-$i"
        $producedAt = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ss.fffZ")
        $tenant = "tenant-" + ($rng.Next(1, 4))
        if ($cond -eq "H1_FORGED_SIG") {
            $bodyTenant = $tenant
            $sig = "forged"
        } else {
            $bodyTenant = "tenant-" + ((([int]$tenant.Substring(7)) % 3) + 1)
            $sig = Sign $tenant $reqId $producedAt
        }
        Publish $tenant $bodyTenant $reqId $producedAt $sig | Out-Null
        $ledger += [pscustomobject]@{
            runId = $RunId; seed = $Seed; detector = $Detector; expectedFault = $cond
            requestId = $reqId; tenantId = $bodyTenant; sentAt = (Get-Date).ToString("o")
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
    } | Export-Csv "$out\observed_$cond.csv" -NoTypeInformation
}

$ledger | Export-Csv "$out\ledger.csv" -NoTypeInformation
Write-Host "Run $RunId saved to $out"