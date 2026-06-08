param(
    [string]$Adb = "",
    [string]$PackageName = "com.youandme.diary",
    [string]$OutputDir = "docs/hackathon/bench-results",
    [switch]$Clear
)

$ErrorActionPreference = "Stop"

function Resolve-Adb {
    param([string]$RequestedAdb)
    if (-not [string]::IsNullOrWhiteSpace($RequestedAdb)) {
        return $RequestedAdb
    }
    $candidates = @()
    if ($env:ANDROID_HOME) {
        $candidates += Join-Path $env:ANDROID_HOME "platform-tools/adb.exe"
    }
    if ($env:ANDROID_SDK_ROOT) {
        $candidates += Join-Path $env:ANDROID_SDK_ROOT "platform-tools/adb.exe"
    }
    if ($env:LOCALAPPDATA) {
        $candidates += Join-Path $env:LOCALAPPDATA "Android/Sdk/platform-tools/adb.exe"
    }
    foreach ($candidate in $candidates) {
        if (Test-Path $candidate) {
            return $candidate
        }
    }
    return "adb"
}

$Adb = Resolve-Adb $Adb

if ($Clear) {
    & $Adb logcat -c
    Write-Host "Cleared logcat. Run each benchmark scenario three times, then run this script without -Clear."
    exit 0
}

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$rawLogPath = Join-Path $OutputDir "ymd-benchmark-logcat-$stamp.log"
$memInfoPath = Join-Path $OutputDir "ymd-meminfo-$stamp.txt"
$reportPath = Join-Path $OutputDir "ymd-benchmark-results-$stamp.md"

$logLines = & $Adb logcat -d -v time "YmdBench:I" "*:S"
$benchLines = $logLines | Where-Object { $_ -match "YmdBench" }
Write-Host "Captured YmdBench records: $($benchLines.Count)"
$benchLines | Set-Content -Path $rawLogPath -Encoding UTF8

try {
    & $Adb shell dumpsys meminfo $PackageName | Set-Content -Path $memInfoPath -Encoding UTF8
} catch {
    "Failed to collect dumpsys meminfo: $($_.Exception.Message)" | Set-Content -Path $memInfoPath -Encoding UTF8
}

function Parse-BenchLine {
    param([string]$Line)
    if ($Line -notmatch "YmdBench(?:\(\d+\))?:\s+(?<payload>.*)$") {
        return $null
    }
    $data = [ordered]@{
        timestamp = ($Line.Substring(0, [Math]::Min(18, $Line.Length))).Trim()
    }
    foreach ($part in $Matches.payload -split "\s+") {
        if ($part -match "^(?<key>[^=]+)=(?<value>.*)$") {
            $data[$Matches.key] = $Matches.value
        }
    }
    [pscustomobject]$data
}

function To-Number {
    param($Value)
    $number = 0.0
    if ([double]::TryParse([string]$Value, [ref]$number)) {
        return $number
    }
    return $null
}

function Average-Field {
    param($Items, [string]$Name)
    $values = @($Items | ForEach-Object { To-Number $_.$Name } | Where-Object { $_ -ne $null -and $_ -ge 0 })
    if ($values.Count -eq 0) {
        return ""
    }
    return [Math]::Round(($values | Measure-Object -Average).Average, 1)
}

function Scenario-Name {
    param($Item)
    if ($Item.op -eq "voice_transcription" -and $Item.mode -eq "offline") { return "端侧语音转录" }
    if ($Item.op -eq "voice_transcription" -and $Item.mode -eq "online") { return "在线语音转录" }
    if ($Item.op -eq "diary_generation" -and $Item.mode -eq "offline" -and $Item.hasImage -eq "true") { return "端侧图文推理" }
    if ($Item.op -eq "diary_generation" -and $Item.mode -eq "offline") { return "端侧文本推理" }
    if ($Item.op -eq "diary_generation" -and $Item.mode -eq "online" -and $Item.hasImage -eq "true") { return "在线图文推理" }
    if ($Item.op -eq "diary_generation" -and $Item.mode -eq "online") { return "在线文本推理" }
    return "$($Item.mode) $($Item.op)"
}

$items = @($benchLines | ForEach-Object { Parse-BenchLine $_ } | Where-Object { $_ -ne $null })
$groups = $items | Group-Object { Scenario-Name $_ }

$markdown = New-Object System.Collections.Generic.List[string]
$markdown.Add("# You & Me Diary Benchmark Results")
$markdown.Add("")
$markdown.Add("- Generated at: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')")
$markdown.Add("- Raw logcat: $rawLogPath")
$markdown.Add("- Meminfo snapshot: $memInfoPath")
$markdown.Add('- Note: summary averages use the latest three successful records for each scenario when available; older records remain in the detail tables.')
$markdown.Add('- Note: `graphicsKb` comes from Android `Debug.MemoryInfo summary.graphics`; dedicated GPU memory is not exposed as a stable public app API.')
$markdown.Add("")

if ($items.Count -eq 0) {
    $markdown.Add('No `YmdBench` records found. Make sure the app build includes benchmark logging and run the scenarios before collecting logcat.')
} else {
    $markdown.Add("## Summary")
    $markdown.Add("")
    $markdown.Add("| Scenario | Runs | OK | Used | Avg elapsed ms | Avg init ms | Avg inference ms | Avg model total ms | Avg total PSS KB | Avg native PSS KB | Avg dalvik PSS KB | Avg graphics KB |")
    $markdown.Add("| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |")
    foreach ($group in $groups) {
        $records = @($group.Group)
        $successRecords = @($records | Where-Object { $_.status -eq "ok" })
        $ok = $successRecords.Count
        $avgSource = if ($ok -gt 0) { @($successRecords | Select-Object -Last 3) } else { $records }
        $markdown.Add("| $($group.Name) | $($records.Count) | $ok | $($avgSource.Count) | $(Average-Field $avgSource 'elapsedMs') | $(Average-Field $avgSource 'initMs') | $(Average-Field $avgSource 'inferenceMs') | $(Average-Field $avgSource 'modelTotalMs') | $(Average-Field $avgSource 'totalPssKb') | $(Average-Field $avgSource 'nativePssKb') | $(Average-Field $avgSource 'dalvikPssKb') | $(Average-Field $avgSource 'graphicsKb') |")
    }
    $markdown.Add("")

    foreach ($group in $groups) {
        $markdown.Add("## $($group.Name)")
        $markdown.Add("")
        $markdown.Add("| # | Time | Status | Elapsed ms | Init ms | Inference ms | Model total ms | Total PSS KB | Native PSS KB | Dalvik PSS KB | Graphics KB | Source/backend |")
        $markdown.Add("| ---: | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- |")
        $index = 1
        foreach ($record in $group.Group) {
            $source = (@($record.source, $record.backend) | Where-Object { $_ }) -join "/"
            $markdown.Add("| $index | $($record.timestamp) | $($record.status) | $($record.elapsedMs) | $($record.initMs) | $($record.inferenceMs) | $($record.modelTotalMs) | $($record.totalPssKb) | $($record.nativePssKb) | $($record.dalvikPssKb) | $($record.graphicsKb) | $source |")
            $index += 1
        }
        $markdown.Add("")
    }
}

$markdown | Set-Content -Path $reportPath -Encoding UTF8
Write-Host "Wrote report: $reportPath"
Write-Host "Wrote raw logcat: $rawLogPath"
Write-Host "Wrote meminfo: $memInfoPath"
