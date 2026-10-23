param([Parameter(Mandatory)][string]$RunDir)

$ledger = Import-Csv "$RunDir\ledger.csv"
$observed = Get-ChildItem "$RunDir\observed_*.csv" | ForEach-Object { Import-Csv $_.FullName }

$byId = @{}
foreach ($o in $observed) { $byId[$o.requestId] = $o }

$rows = foreach ($group in ($ledger | Group-Object expectedFault)) {
    $prevTenant = $null
    foreach ($l in $group.Group) {
        $o = $byId[$l.requestId]
        $matched = $null -ne $o
        if ($group.Name -eq "NONE") {
            $expected = $false
        } elseif ($group.Name -eq "F2_STALE_REUSE") {
            # Stale leftover is a violation only if the previous message on the worker had a different tenant
            $expected = ($null -ne $prevTenant) -and ($prevTenant -ne $l.tenantId)
        } else {
            $expected = $true
        }
        $prevTenant = $l.tenantId
        [pscustomobject]@{
            condition        = $group.Name
            requestId        = $l.requestId
            matched          = $matched
            expectedPositive = $expected
            detected         = ($matched -and $o.detected -eq "True")
            observedFault    = if ($matched) { $o.observedFault } else { "UNMATCHED" }
        }
    }
}

$rows | Export-Csv "$RunDir\scored.csv" -NoTypeInformation

$summary = $rows | Group-Object condition | ForEach-Object {
    $g = $_.Group
    [pscustomobject]@{
        condition = $_.Name
        n         = $g.Count
        TP        = @($g | Where-Object { $_.expectedPositive -and $_.detected }).Count
        FN        = @($g | Where-Object { $_.expectedPositive -and -not $_.detected }).Count
        FP        = @($g | Where-Object { -not $_.expectedPositive -and $_.detected }).Count
        TN        = @($g | Where-Object { -not $_.expectedPositive -and -not $_.detected }).Count
        unmatched = @($g | Where-Object { -not $_.matched }).Count
    }
}

$summary | Export-Csv "$RunDir\summary.csv" -NoTypeInformation
$summary | Format-Table -AutoSize