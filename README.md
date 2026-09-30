# 观潮 · 原生 Android

BTC / ETH / SOL 行情与研究应用。Android 原生 Views 和 Canvas 渲染，无 WebView、网页或 JavaScript 运行时。

## 0.7 重构分支

- 行情、研报、看懂、ETF、提醒五个入口。统一卡片、留白、图标导航和字号；跟随系统浅深色，价格单行自适应，大段图表数据与解读可展开。
- 移除三维模型、实验滑块和分步虚拟课堂。保留九个概念的一句话解释、例子、市场影响和常见误解。
- 报价独立队列，前台正常连接时约 3 秒获取一次；K 线约 15 秒、其他周期约 1 分钟。慢请求会延长间隔，这是 HTTPS 轮询，尚非 WebSocket 逐笔推送。后台停止报价轮询。
- ETF 可查看 1 个月、3 个月、1 年或全部，展示每日柱状图、完整披露日累计与分基金数字，分批加载更早记录；前台每 5 分钟检查新披露。尝试读取 Farside 公开历史表，保留已有缓存历史；拒绝变化或无法核对的表格。未披露不算零；累计排除未完整披露日。
- 开启提醒或授予权限后立即申请一次系统检查，提供系统通知、后台和电池设置入口。vivo / iQOO 设备显示 OriginOS 自启动和后台设置说明。持续后台检查仍由 Android JobScheduler 调度，不代表即时云推送已接通。
- 原生 XML 页面骨架可在 Android Studio 的 Split / Design 里预览。打开 `app/src/main/res/layout/screen_shell.xml`；预览占位内容不是行情。实际图表与交互用模拟器和 Layout Inspector 查看。

Android 8.0 及以上，包名 `com.tide.journal`。几百 KB 来自复用系统原生组件，不使用无意义资产增大安装包。

## 构建与验证

运行 `bash tests/run-domain.sh`，覆盖行情指标与 ETF 表格缺失值、列变更、日期范围、完整历史和异常响应。

使用官方 Android SDK 36 / Build Tools 36 和 Java 17：

`python build.py /path/to/sdk --key /private/path/existing-test.jks`

`python tests/build-instrumentation.py /path/to/sdk /private/path/existing-test.jks`

`python tests/verify-package.py`

1080×2400 的 Android 模拟器额外覆盖普通浅色、普通深色、1.3 倍字体三组界面，检查导航触控尺寸、文字溢出和对比度，并导出真实截图；这些结果不代表 OriginOS 真机已验收。详见 [OriginOS 与外观验证](docs/ORIGINOS.md)。

GitHub Actions 从当前提交源码重新编译，并使用每次任务临时生成的 **CI 验证签名** 运行模拟器、原生交互和通知测试。生成的 APK 与收据保存在任务 artifact 中；CI 不自动提交 APK，不覆盖已发布测试包。CI 签名不能升级已有私有测试签名的安装，新源码的可升级安装包需要既有测试密钥。域测试、构建、模拟器和真实云推送送达分别验收。

已发布的 0.6 原生测试包仍可从[固定历史提交下载](https://github.com/makalongshiwo-sys/makalong/raw/1fefb0897699f73184494dffdcd0c342d91facee/releases/guanchao-native-preview.apk)，其中尚不包含本分支重构。仓库原有 `releases/` 与 `evidence/` 回执对应此前版本；新分支结果以当前提交 Actions artifact 为准。

## 仍需接通

- 服务端持续采集、FCM / 厂商推送配置与真机后台送达验证。系统检查可能受省电、离线和强制停止影响。
- ETF 公开网页可能拒绝访问或更改表格；需以 `etf-source-probe.json` 核对当前来源访问与解析结果。失败时只显示带日期的快照或缓存，不声称历史已补全。公开日度 ETF 数据不是秒级行情。
- 既有签名的升级包和用户手机上的外观、耗电、长时间后台验证。

## 研究发布

在维护本项目的会话中明确请求“分析 BTC，并更新”，核验来源与时间后追加到 `content/reports.json`，提交并读回 revision。App 自动读取已发布研究，用户无需导入；行情更新不会自动发布研究。详见 [PUBLISHING.md](PUBLISHING.md)。

公开仓库不保存账户、仓位、成本、密钥或聊天原文。
