# 站点入口 Android

用于封装多个 Web 网站的 Android WebView 应用。

## 功能

- 首次启动显示空站点列表，用户自行添加网站地址。
- 站点可设置备注名称、协议、IP 或域名、端口和路径。
- 支持多个站点，长按站点卡片可编辑。
- 可设置默认站点，启动时自动进入。
- WebView 持久化 Cookie；网站登录成功后，下次进入会恢复登录会话。
- 网站页可刷新、清空网页缓存或清除当前 WebView 登录状态。
- 网页页提供悬浮气泡菜单，可切换网站、刷新、清空网页缓存和清除登录状态。

## 构建

在安装 Android Studio 后打开 `SiteHubAndroid` 目录，等待 Gradle 同步完成，再执行 `Build > Build APK(s)`。

生成的发布 APK 位于 `app/build/outputs/apk/release/app-release.apk`。发布签名由 GitHub Actions Secrets 管理。

推送到 `main` 后，GitHub Actions 的 `Build Android APK` 工作流会自动生成发布 APK。构建成功后可在对应工作流运行的 Artifacts 中下载 `SiteHub-release-apk`。

后续版本必须继续使用同一签名文件，并递增 `versionCode`，这样 Android 才能覆盖升级。v1.0.0 使用旧的调试签名，首次安装持久签名版本可能需要卸载旧版；从 v1.3.0 开始使用持久发布签名。

项目允许明文 HTTP 站点访问，因此仅应添加受信任的内网或自有站点。
