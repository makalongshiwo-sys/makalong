# 观潮 · 原生 Android

BTC / ETH / SOL 研究应用的原生 Android 测试版。Android Views 负责界面，Canvas 绘制 K 线，OpenGL ES 2.0 渲染课堂，Java 计算指标，SQLite 保存缓存与提醒。APK 不包含 WebView、HTML 或 JavaScript 运行时。

[下载原生测试版 APK](https://github.com/makalongshiwo-sys/makalong/raw/1fefb0897699f73184494dffdcd0c342d91facee/releases/guanchao-native-preview.apk) · [构建与设备验证](https://github.com/makalongshiwo-sys/makalong/actions/runs/35805327741)

Android 8.0 及以上。包名 `com.tide.journal`，版本 `0.6-native-preview`，沿用 V5 私有测试签名。安装生产测试包 `guanchao-native-preview.apk` 即可；`native-instrumentation.apk` 是 CI 专用测试程序。体积来自系统原生组件复用，不包含资源填充；体积不作为视觉质量或完成度的验收依据。

## 本版可用

- BTC、ETH、SOL 六周期图表：1h / 4h / 8h / 12h / 日线 / 周线。双指缩放、平移、选中 K 线，显示 OHLC、BB20、MACD12/26/9、RSI14 数值与解释。默认解读最近已闭合线，未闭合和缓存状态分别标记。
- 固定箱体、两根闭合线确认、突破失败与价格强弱候选。缺口重新预热；指标不是完整牛熊研判或交易指令。
- 九门课堂：加息、降息、需求通胀、供给冲击、就业、ETF、预期差、未平仓量、杠杆。可拖动三维场景、调参数、展开模型、点数值看公式、分步学习；示例不预测资产价格。
- 日报/周报历史、报告来源与复核期限、报告内事件和解释检索。远端读取失败时保留有日期的缓存；内置内容是使用指南，不冒充实时研究。
- ETF 分基金披露快照、公开 OI/资金费率接口、预测市场议题及期限。缺失 ETF 数字显示缺失，不当作零。
- 原生通知权限、测试通知、闭合事件提醒、收件箱与 SQLite 去重；可配置各类提醒。

## 更新方式

在有本仓库连接的维护对话中说“分析 BTC，并更新”，助手核验本次资料、追加公开研究到 `content/reports.json`，提交 GitHub 并读回 revision。App 自动读取已发布报告，用户无需导入文件；研究内容更新无需安装新 APK。详见 [PUBLISHING.md](PUBLISHING.md)。

行情页前台约 15 秒拉取所选市场；研报页前台约 1 分钟检查发布内容。启用后台提醒后使用 Android JobScheduler 定期检查，实际时机由系统调度。后台检查不会自动生成新的研究，也不能监听其他 ChatGPT 会话。

## 尚未完成

- FCM / 手机厂商即时云推送、服务端持续行情采集和真实设备送达验收。现有本地定期检查可能受省电与网络影响而延迟。
- ETF 供应商自动披露同步。当前保留披露日和采集日的快照，不是全天候实时 ETF 数据源。
- 任意问题的在线生成式问答；本版是报告内解释检索和可互动教材。
- 用户手机上的外观、高刷新率、耗电和长时间后台测试。模拟器不证明真机达到 120 Hz。

## 构建与验收

`bash tests/run-domain.sh` 运行指标与时间边界回归，当前 3,860 项断言。`python tests/verify-package.py` 核验源码与 APK 哈希和无网页资产结构。设备工作流在 Android 15 模拟器安装仓库内同一签名 APK，操作真实原生控件、检查系统通知，并运行单独的签名 instrumentation 覆盖全部九门课堂和 SQLite 去重。

2026-09-23 的 [设备回执](evidence/ci/device-result.json) 与本下载包 `cf608b02661e3ad175d9fad737a0b39d74ed0cd6f6026bff9e4cee1932a4ddb4` 一致，完整工作流通过，额外设备内检查 31 项通过。[独立审阅](evidence/review/SUMMARY.md) 另有通知调度与缓存失败记录的 18 项隔离断言。实际截图：[图表测试夹具](evidence/ci/instrumentation/instrumented-chart-fixture.png)、[ETF 净流出](evidence/ci/instrumentation/instrumented-lesson-etf.png)、[降息利息差](evidence/ci/instrumentation/instrumented-lesson-cuts.png)。课堂和夹具数字是教学/测试例子。

完整测试视频流未录制；截图来自实际 Android 模拟器。其余截图与原始运行日志保存在该次 Actions artifact（2026-10-23 到期），[摘要与哈希](evidence/device-artifact.json) 长期保留在仓库。[验收清单](evidence/improvement-manifest.json) 将完整项目标记为 partial，不把未接通的线上服务算作完成。

完整 Gradle 工程位于仓库根目录；另提供可直接使用 Android SDK Build Tools 36 和 Java 17 的 `build.py`。构建必须显式提供既有测试密钥，例如 `python build.py /path/to/sdk --key /private/path/test.jks`，不能把私钥提交到公开仓库。`releases/release.json` 记录安装包与每个应用源文件的哈希。设备证据必须与该 APK 哈希一致才可用于本包验收。

仓库不保存个人账户、成本、仓位、密钥或聊天原文。原网站与旧原型保留，原生应用由本仓库继续迭代。
