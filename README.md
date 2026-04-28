# 系统设置助手 · OpenWifi

> Android TV 快捷设置工具 —— WiFi 自动保持 + 一键跳转常用系统设置

![Android](https://img.shields.io/badge/Android-6.0%2B-green?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue?logo=kotlin)
![minSdk](https://img.shields.io/badge/minSdk-23-brightgreen)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

---

## 功能特性

| 功能 | 说明 |
|------|------|
| **WiFi 状态展示** | 实时显示 WiFi 开/关状态及已连接的 SSID |
| **一键开启 WiFi** | WiFi 关闭时显示开启按钮，Android 9 及以下直接开启，Android 10+ 跳转设置页 |
| **WiFi 设置** | 快速跳转系统 WiFi 设置页 |
| **输入法设置** | 快速跳转系统输入法设置页 |
| **更多系统设置** | 快速跳转系统设置主页 |
| **WiFi 自动保持** | 后台常驻服务，检测到 WiFi 关闭时自动重新开启（Android 9 及以下） |
| **开机自启** | 系统开机后自动启动监控服务，无需手动打开 App |

## 截图预览

> TV 横屏两栏式布局，左侧状态信息，右侧快捷操作按钮
>
> 配色方案参考微信（主色 `#07C160`），深色背景减少 TV 屏幕反光

## 环境要求

| 项目 | 要求 |
|------|------|
| Android | 6.0 (API 23) 及以上 |
| 目标平台 | Android TV（同时兼容手机/平板） |
| 构建工具 | Android Studio / Gradle 8.4 |
| JDK | 17 |
| Kotlin | 1.9.22 |

## 快速开始

### 克隆项目

```bash
git clone https://github.com/pengjunlong/open_wifi.git
cd open_wifi
```

### 本地构建

```bash
# Debug APK
./gradlew assembleDebug

# Release APK（需配置签名，见下方）
./gradlew assembleRelease
```

构建产物位于 `app/build/outputs/apk/`

### 安装到设备

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 项目结构

```
open_wifi/
├── app/src/main/
│   ├── kotlin/com/example/openwifi/
│   │   ├── MainActivity.kt         # 主界面：状态展示 + 按钮跳转
│   │   ├── WifiMonitorService.kt   # 前台常驻 Service：WiFi 自动保持
│   │   └── BootReceiver.kt         # 开机广播：自动启动监控服务
│   ├── res/
│   │   ├── layout/activity_main.xml  # TV 横屏两栏布局
│   │   ├── values/colors.xml         # 微信风格配色
│   │   ├── values/strings.xml        # 文案
│   │   └── values/styles.xml         # 主题
│   └── AndroidManifest.xml
├── .github/workflows/ci.yml         # GitHub Actions CI/CD
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## CI / CD

项目使用 GitHub Actions 自动构建，详见 [`.github/workflows/ci.yml`](.github/workflows/ci.yml)。

| 触发条件 | 执行任务 |
|----------|----------|
| PR → `main` / `develop` | Lint 检查 + Debug APK 构建 |
| 推送 Tag `v*.*.*` | 签名 Release APK 构建 + 创建 GitHub Release |

### Release 签名配置

在仓库 **Settings → Secrets and variables → Actions** 中添加：

| Secret | 说明 |
|--------|------|
| `KEYSTORE_BASE64` | `base64 -i your.jks` 的输出 |
| `KEYSTORE_PASSWORD` | Keystore 密码 |
| `KEY_ALIAS` | Key alias |
| `KEY_PASSWORD` | Key 密码 |

### 发布新版本

```bash
git tag v1.0.0
git push origin v1.0.0
```

推送 Tag 后 GitHub Actions 自动构建签名 APK 并创建 Release。

## 权限说明

| 权限 | 用途 |
|------|------|
| `ACCESS_WIFI_STATE` | 读取 WiFi 状态和已连接 SSID |
| `CHANGE_WIFI_STATE` | 开启/关闭 WiFi |
| `ACCESS_NETWORK_STATE` | 检测网络连接状态 |
| `FOREGROUND_SERVICE` | 前台 Service（Android 9+） |
| `RECEIVE_BOOT_COMPLETED` | 开机自启 |
| `POST_NOTIFICATIONS` | 前台 Service 常驻通知（Android 13+） |

## 注意事项

- **Android 10+**：系统限制第三方应用无法直接调用 `setWifiEnabled()`，WiFi 关闭时会引导用户前往设置页手动开启。TV 设备通常为 Android 9 以下，不受此限制。
- **前台 Service**：常驻通知栏会显示「WiFi 自动保持服务」，优先级为 LOW，不会发出声音或震动。

## License

[MIT](LICENSE)

