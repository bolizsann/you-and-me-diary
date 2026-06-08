# 端侧 / 在线 Benchmark 摘要

测试时间：2026-06-08 00:38-00:48  
测试设备：vivo X300s，天玑 9500，16GB + 16GB RAM，1TB 存储  
原始报告：`docs/hackathon/bench-results/ymd-benchmark-results-20260608-005420.md`

> 说明：统计使用每个场景最新 3 条成功记录。`CPU 侧内存` 约等于 `nativePss + dalvikPss`；`Graphics/GPU 相关内存` 来自 Android `Debug.MemoryInfo summary.graphics`，不是底层 dedicated GPU memory。

| 场景 | 成功样本 | 平均总耗时 | 平均模型推理耗时 | CPU 侧内存 | Graphics/GPU 相关内存 | 结果来源 |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| 端侧语音转录 | 3/3 | 1.63s | 1.63s | 558MB | 2376MB | LiteRT-LM GPU |
| 端侧图文推理 | 3/3 | 3.77s | 3.73s | 514MB | 2548MB | local-gemma-gpu |
| 在线语音转录 | 3/3 | 9.68s | N/A | 415MB | 2438MB | Cloud Run |
| 在线图文推理 | 3/3 | 2.75s | N/A | 358MB | 2545MB | Cloud Run / gemma |

补充观察：

- 端侧语音转录 3 次均命中 GPU backend，平均耗时约 1.6s。
- 端侧图片 + 文本图卡生成 3 次均为 `local-gemma-gpu`，平均总耗时约 3.8s。
- 在线语音转录受网络与 Cloud Run 链路影响，耗时波动大于端侧。
- 在线图文推理在 Wi-Fi 环境下曾出现 3 次 socket abort，落到本地 fallback；关闭 Wi-Fi 后 3 次均成功返回 `source=gemma`，有效统计使用成功样本。
