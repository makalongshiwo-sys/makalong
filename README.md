# 观潮 · 原生 Android

[下载观潮 0.9 APK](https://raw.githubusercontent.com/makalongshiwo-sys/makalong/codex/guanchao-0.9-android16/releases/guanchao-0.9.apk) · [远程提醒设置](docs/PUSH-SETUP.md) · [当前源码回执](releases/guanchao-0.9-receipt.json)


BTC / ETH / SOL 行情与研究应用。Android 原生 Views 和 Canvas 渲染，无 WebView、网页或 JavaScript 运行时。

## 0.9 原生应用

独立包名 `com.guanchao.app`，可与旧版并存；保留新包的私有签名备份，后续交付更新复用同一签名。原生青绿色界面、自适应图标、系统深色和大字体。首次启动可立即回看带时间的行情，联网后替换为最新来源数据，历史快照不触发新信号。

手动开启实时盯盘，以可见常驻通知监测 BTC / ETH / SOL 波动、1h / 4h 闭合与指标。短时波动冷却按币种持久保存；普通划掉最近任务后可继续，系统回收进程时会在剩余时长内尝试恢复。另外提供无需 Google 的远程提醒：原生 specialUse 前台订阅 ntfy 唤醒，再从项目 HTTPS 公共行情接口验证新事件，由观潮自己通知。云端任务运行期间约每分钟检查，任务间调度可能延迟；手机须能直接访问 ntfy.sh 与 GitHub API。厂商彻底清理服务或强行停止仍可能中断。Android 15/16 后台 dataSync 有每日时长限制，本次会话最多 6 小时。使用与限制见 [安装说明](INSTALL-0.9.md)，手机后台设置、架构与 GitHub 参考见 [远程提醒说明](docs/PUSH-SETUP.md)。

验证以本版本的签名、源码散列及 Android 16 回执为准，包括新旧并存、同签名重装保留记录、两轮真实后台报价刷新、删除最近任务后的通知管线、三种外观与完整领域回归；不代表 vivo / OnePlus 真机清理后送达已验收。

## 原有功能

- 行情、研报、看懂、ETF、提醒五个入口。统一卡片、留白、图标导航和字号；跟随系统浅深色，价格单行自适应，大段图表数据与解读可展开。
- 移除三维模型、实验滑块和分步虚拟课堂。保留九个概念的一句话解释、例子、市场影响和常见误解。
- 报价独立队列，前台正常连接时约 3 秒获取一次；K 线约 15 秒、其他周期约 1 分钟。慢请求会延长间隔，这是 HTTPS 轮询，尚非 WebSocket 逐笔推送。前台 Activity 暂停时取消界面请求；单独开启的盯盘服务可以继续读取。
- ETF 可查看 1 个月、3 个月、1 年或全部，展示每日柱状图、完整披露日累计与分基金数字，分批加载更早记录；前台每 5 分钟检查新披露。尝试读取 Farside 公开历史表，保留已有缓存历史；拒绝变化或无法核对的表格。未披露不算零；累计排除未完整披露日。
- 开启提醒或授予权限后立即申请一次系统检查，提供系统通知、后台和电池设置入口。vivo / iQOO 设备显示 OriginOS 自启动和后台设置说明。持续后台检查仍由 Android JobScheduler 调度，不代表即时云推送已接通。
- 原生 XML 页面骨架可在 Android Studio 的 Split / Design 里预览。打开 `app/src/main/res/layout/screen_shell.xml`；预览占位内容不是行情。实际图表与交互用模拟器和 Layout Inspector 查看。

Android 8.0 及以上，包名 `com.guanchao.app`。几百 KB 来自复用系统原生组件，不使用无意义资产增大安装包。

## 构建与验证

运行 `bash tests/run-domain.sh`，覆盖行情指标与 ETF 表格缺失值、列变更、日期范围、完整历史和异常响应。

使用官方 Android SDK 36 / Build Tools 36 和 Java 17：

`python build.py /path/to/sdk --key /private/path/existing-test.jks`

`python tests/build-instrumentation.py /path/to/sdk /private/path/existing-test.jks`

`python tests/verify-package.py`

1080×2400 的 Android 模拟器额外覆盖普通浅色、普通深色、1.3 倍字体三组界面，检查导航触控尺寸、文字溢出和对比度，并导出真实截图；这些结果不代表 OriginOS 真机已验收。详见 [OriginOS 与外观验证](docs/ORIGINOS.md)。

GitHub Actions 重新编译当前源码并在 Android 16 模拟器验证。签名私钥仅以持有者 RSA 公钥加密备份，接受该轮安装包前恢复并核对其证书；不写入公开仓库。后续安装包必须复用私有恢复的 JKS，不能把其他轮次的 CI 签名直接当成可升级发布包。

## 仍需接通

- vivo / OnePlus 真机的一键清理、锁屏与境内无 VPN 送达实测。第三方公共通道与 GitHub 的网络可达性仍取决于手机网络。
- ETF 公开网页可能拒绝访问或更改表格；需以 `etf-source-probe.json` 核对当前来源访问与解析结果。失败时只显示带日期的快照或缓存，不声称历史已补全。公开日度 ETF 数据不是秒级行情。
- 旧包 com.tide.journal 的原私钥仍未提供；用户手机上的外观、耗电和清理后长时间后台送达仍需真机验证。

## 研究发布

在维护本项目的会话中明确请求“分析 BTC，并更新”，核验来源与时间后追加到 `content/reports.json`，提交并读回 revision。App 自动读取已发布研究，用户无需导入；行情更新不会自动发布研究。详见 [PUBLISHING.md](PUBLISHING.md)。

公开仓库不保存账户、仓位、成本、密钥或聊天原文。
