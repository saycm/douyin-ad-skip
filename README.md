# 抖音跳广告

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

面向 LSPosed 的抖音、抖音极速版小程序及小游戏广告跳过模块。

当前版本：`1.3`（versionCode `6`）

模块包名：`com.inkdye.fuckminiappad`

## 适配范围

| 应用 | 版本 | 模块入口 |
| --- | --- | --- |
| 抖音 | 41.1.0 (`410101`) | 历史混淆入口及小游戏入口 |
| 抖音 | 41.2.0 (`410200`) | 新增混淆入口及小游戏入口，完成后静态核对 |
| 抖音极速版 | 41.1.0 (`410101`) | 延迟加载插件入口及小游戏入口 |

作用域包名：

```text
com.ss.android.ugc.aweme
com.ss.android.ugc.aweme.lite
```

## 模块改动

- 增加抖音极速版作用域 `com.ss.android.ugc.aweme.lite`。
- 监听 Mira `PluginLoader.LJ`，在 `m.l.miniapp` 插件加载完成后取得对应类加载器。
- 挂钩普通版 `MiniAppAdManagerServiceImpl`、`GameAdManagerService` 的历史混淆入口和 41.2 新入口。
- 挂钩极速版 `MiniAppAdManagerServiceImpl.operateVideoAd/2`、`GameAdManagerService.operateVideoAd/2`、`operateVideoAd/3` 和 `operateVideoAdIntercept/3`。
- 回调兼容普通版的 `LIZIZ`、`LJ` 字段和方法，以及极速版的 `type`、`onSuccess`。
- 仅在广告事件类型为 `show` 时跳过原广告流程，并依次发送 `show`、奖励成功和 `close` 状态。
- `load` 等其他事件继续交给原方法处理。
- 根据参数类型和回调能力筛选目标方法，并防止同一方法重复挂钩。

一次成功拦截的模块日志顺序为：

```text
emitted state=show
success callback completed
skipped operateVideoAd show and returned reward state
emitted state=close
```

## 安装

环境要求：

- Android 10 及以上
- 已安装并启用 LSPosed
- 对应应用中存在小程序或小游戏功能

安装签名后的 APK，在 LSPosed 中启用模块，并勾选抖音和抖音极速版作用域。修改作用域后，强制停止对应应用再重新打开。

小游戏进程通常为：

```text
com.ss.android.ugc.aweme:minigame0
com.ss.android.ugc.aweme.lite:minigame0
```

## 构建

环境要求：

- Android SDK Platform 36
- Android Build Tools 36.0.0
- JDK 17
- Python 3

设置 Android SDK 路径后执行：

```powershell
$env:ANDROID_HOME = "<Android SDK 路径>"
python tools\build.py
```

未签名产物：

```text
build/FuckMiniappAD_Multi_1.3-unsigned.apk
```

仓库不包含签名密钥。使用自己的密钥完成 APK 签名。

## 测试

运行宿主 JVM 回归测试：

```powershell
python tests\test_compat_runtime.py
```

测试覆盖内容：

- 普通版 41.1.0 和 41.2.0 广告入口
- 极速版插件已加载和延迟加载
- 极速版小程序及小游戏入口
- `load` 事件保持原逻辑
- 重复插件加载和无关方法重载

测试使用模拟宿主类，验证模块分支逻辑。

## 已知限制

- 目标应用更新后可能修改类名、方法签名、插件名称或回调协议，需要重新核对目标代码。
- 奖励状态由小程序服务端和页面逻辑共同决定，模块只处理客户端广告播放入口。
- 抖音 41.2.0 已完成静态入口核对。

## 许可证

本项目采用 [GNU General Public License v3.0](LICENSE)。重新分发修改版本时，请保留许可证、版权声明和对应源码。

## 参考

- [LSPosed API](https://libxposed.github.io/api/)
- [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.html)
