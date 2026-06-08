# You & Me Diary Benchmark Results

- Generated at: 2026-06-08 00:54:21
- Raw logcat: docs\hackathon\bench-results\ymd-benchmark-logcat-20260608-005420.log
- Meminfo snapshot: docs\hackathon\bench-results\ymd-meminfo-20260608-005420.txt
- Note: summary averages use the latest three successful records for each scenario when available; older records remain in the detail tables.
- Note: `graphicsKb` comes from Android `Debug.MemoryInfo summary.graphics`; dedicated GPU memory is not exposed as a stable public app API.

## Summary

| Scenario | Runs | OK | Used | Avg elapsed ms | Avg init ms | Avg inference ms | Avg model total ms | Avg total PSS KB | Avg native PSS KB | Avg dalvik PSS KB | Avg graphics KB |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 端侧语音转录 | 3 | 3 | 3 | 1628.3 | 0 | 1625 | 1625.3 | 3407175 | 556736.3 | 14685 | 2432746.7 |
| 端侧图文推理 | 3 | 3 | 3 | 3771.3 | 0 | 3727.7 | 3727.7 | 3675834.3 | 516614.7 | 9273.7 | 2609502.7 |
| 在线语音转录 | 6 | 6 | 3 | 9679.3 |  |  |  | 3396353 | 404943 | 20310.3 | 2495997.3 |
| 在线图文推理 | 6 | 3 | 3 | 2746 |  |  |  | 3448294.7 | 355543.3 | 11270 | 2606458.7 |

## 端侧语音转录

| # | Time | Status | Elapsed ms | Init ms | Inference ms | Model total ms | Total PSS KB | Native PSS KB | Dalvik PSS KB | Graphics KB | Source/backend |
| ---: | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- |
| 1 | 06-08 00:38:46.858 | ok | 1737 | 0 | 1734 | 1734 | 3451291 | 578183 | 10343 | 2432644 | gpu |
| 2 | 06-08 00:39:01.014 | ok | 1419 | 0 | 1415 | 1415 | 3378061 | 546119 | 12932 | 2432840 | gpu |
| 3 | 06-08 00:39:14.331 | ok | 1729 | 0 | 1726 | 1727 | 3392173 | 545907 | 20780 | 2432756 | gpu |

## 端侧图文推理

| # | Time | Status | Elapsed ms | Init ms | Inference ms | Model total ms | Total PSS KB | Native PSS KB | Dalvik PSS KB | Graphics KB | Source/backend |
| ---: | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- |
| 1 | 06-08 00:39:50.444 | ok | 3964 | 0 | 3924 | 3924 | 3667660 | 548054 | 7575 | 2546044 | local-gemma-gpu/gpu |
| 2 | 06-08 00:40:22.070 | ok | 3674 | 0 | 3636 | 3636 | 3666625 | 514007 | 9391 | 2609256 | local-gemma-gpu/gpu |
| 3 | 06-08 00:40:37.458 | ok | 3676 | 0 | 3623 | 3623 | 3693218 | 487783 | 10855 | 2673208 | local-gemma-gpu/gpu |

## 在线语音转录

| # | Time | Status | Elapsed ms | Init ms | Inference ms | Model total ms | Total PSS KB | Native PSS KB | Dalvik PSS KB | Graphics KB | Source/backend |
| ---: | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- |
| 1 | 06-08 00:41:34.244 | ok | 17387 |  |  |  | 3687794 | 554523 | 17221 | 2595652 |  |
| 2 | 06-08 00:41:55.299 | ok | 7397 |  |  |  | 3519488 | 538582 | 25436 | 2443292 |  |
| 3 | 06-08 00:42:26.650 | ok | 20898 |  |  |  | 3528694 | 529171 | 33796 | 2445344 |  |
| 4 | 06-08 00:47:24.254 | ok | 11890 |  |  |  | 3489977 | 404779 | 12973 | 2597152 |  |
| 5 | 06-08 00:47:46.776 | ok | 8644 |  |  |  | 3346034 | 404963 | 20373 | 2445488 |  |
| 6 | 06-08 00:48:12.245 | ok | 8504 |  |  |  | 3353048 | 405087 | 27585 | 2445352 |  |

## 在线图文推理

| # | Time | Status | Elapsed ms | Init ms | Inference ms | Model total ms | Total PSS KB | Native PSS KB | Dalvik PSS KB | Graphics KB | Source/backend |
| ---: | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- |
| 1 | 06-08 00:42:51.347 | null | 3888 |  |  |  | 3541112 | 471703 | 11952 | 2545064 |  |
| 2 | 06-08 00:43:11.367 | null | 785 |  |  |  | 3554477 | 422279 | 9072 | 2610824 |  |
| 3 | 06-08 00:43:36.440 | null | 2513 |  |  |  | 3637148 | 412579 | 12372 | 2695120 |  |
| 4 | 06-08 00:45:48.915 | ok | 3172 |  |  |  | 3451204 | 354756 | 10318 | 2610944 | gemma |
| 5 | 06-08 00:46:20.576 | ok | 2661 |  |  |  | 3487468 | 356015 | 11270 | 2645260 | gemma |
| 6 | 06-08 00:46:36.237 | ok | 2405 |  |  |  | 3406212 | 355859 | 12222 | 2563172 | gemma |

