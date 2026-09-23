# 独立只读复核 — 2026-09-23

**结论：上轮两项 P2 源码缺陷已关闭；本次限定复核未发现新的阻断项。当前 APK 的最终模拟器验收仍须与其 SHA-256 相符的回执。** 本审阅只写入 `evidence/review/`，没有修改产品代码。

## 两项修复与实际测试

1. **后台任务被 Activity 重建取消：已修复。** `AlertJob.schedule()` 在提醒启用时保留已有 610 周期任务和 611 单次任务；只有明确停用才取消两者。已有任务重复进入、缺少周期任务但保留单次任务、停用、调度被系统拒绝均覆盖。
2. **报告下载失败未进入日志：已修复。** `AlertJob.check()` 在 `Repository.document()` 回退到本机缓存或随包资料时，将报告下载失败及原因写入 `lastResult`。六项行情成功与报告失败分别保留；缓存不会作为新报告通知。恢复成功后不残留旧错误，停订报告时不请求报告。

`python3 evidence/review/run-alert-review.py` 直接编译当前产品 `AlertJob.java` / `Market.java`，使用明确的 Android、网络、SQLite 边界替身执行 **18 项断言，全部通过**。测试源是 `AlertJobReview.java`；替身、编译与执行流程均保留在 `run-alert-review.py`，不进入产品 APK。它验证控制流，不模拟真实 Android 调度时机或送达。

本次另实际运行：

- `bash tests/run-domain.sh`：**3,860 项断言通过**。
- `python3 tests/verify-package.py`：**当前源码哈希、APK 哈希、无 WebView / 网页运行时资产检查通过**。

原始输出为 `alert-review.txt`、`domain.txt`、`package.txt`；精确源码、测试源哈希与执行上下文在 `receipt.json`。

## 模拟器证据边界

读取时 `evidence/ci/device-result.json` 标记 Android 15、passed，其 APK 是 `42c135d7eb9391c737a35c9434764b8fd8e4eed880a040078e0d3f98c5f29d48`。当前产品包是 `cf608b02661e3ad175d9fad737a0b39d74ed0cd6f6026bff9e4cee1932a4ddb4`。**二者不同，旧回执不能充当当前包的精确验收。** ROOT 正在等待最新 CI；本审阅未把该等待伪报为通过。

独立查看旧 CI 的 `09-reports.png`，可见云端读取及 `cloud-on-request-20260919-v5` revision；`08-etf.png` 可见云端读取、快照时间、交易日及净流量。这支持被测版本的原生展示与读取状态，不代表实时 ETF 供应商接通。旧回执列出的点击和本机通知步骤属于旧包的模拟器证据，不能替代真机或云推送收件回执。

本轮不声称真实手机、Doze/OEM 保活、高刷帧率、FCM/OEM 即时推送或 ETF 自动供应商采集已通过。后续收到与当前 APK 相符的模拟器回执后，应更新本节及 `receipt.json`，保留旧包/新包的归属。

## 追加最终模拟器核验 — 2026-09-23T01:18:46.213921+00:00

**当前包的模拟器证据已对齐，前述“等待当前包回执”的边界已关闭。** 原始 `reviewedAt`、初次结论和旧 `42c135d7…` 回执记录保持不变；本次作为 `verifiedAt` / `currentReceipt` 单独追加。

实际读回 `evidence/device-artifact.json`、`evidence/ci/device-result.json` 与 instrumentation 回执：GitHub run **35805327741** 标记 success，artifact **10727062954**；本地 APK、设备回执及 artifact 元数据均为 `cf608b02661e3ad175d9fad737a0b39d74ed0cd6f6026bff9e4cee1932a4ddb4`。被审阅源码哈希仍全部相符。

Android 15 模拟器回执为 passed；独立 instrumentation 为 **31 checks passed**，原始输出含 `INSTRUMENTATION_CODE: -1`。本次逐项复核 23 份提取截图的哈希与元数据一致，所提供 logcat 中未发现 `FATAL EXCEPTION`。ZIP SHA-256 `ec83d2d29501a073362cdb9008f19270129d4ea9bccd8f035b13c91d86d801d7` 来自 ROOT 已验证的提取元数据，本审阅没有重新下载 ZIP。

独立查看新 ETF 与降息教学截图：ETF 零申购停留在零网格，赎回／净流负值及降息利息差额位于开放网格下方，没有被实体地板遮住。图像来自明确的教学测试夹具，不是实时市场数据。

本包已有模拟器安装、界面点按、课堂滑块、报告／ETF 页面和本机系统通知证据；这不外推为物理手机高刷、Doze/OEM 调度或 FCM/OEM 云推送送达。ETF 自动供应商采集仍未完成。按要求，本次没有新增或重复执行产品测试，只追加了已有证据的独立读回与身份核验。
