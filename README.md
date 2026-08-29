# 站点入口 Android

用于封装多个 Web 网站的 Android WebView 应用。

## 功能

- 首次启动显示空站点列表，用户自行添加网站地址。
- 站点可设置备注名称、协议、IP 或域名、端口和路径。
- 支持多个站点，长按站点卡片可编辑。
- 可设置默认站点，启动时自动进入。
- WebView 持久化 Cookie；网站登录成功后，下次进入会恢复登录会话。
- 网站页可刷新或清除当前 WebView 登录状态。

## 构建

在安装 Android Studio 后打开 `SiteHubAndroid` 目录，等待 Gradle 同步完成，再执行 `Build > Build APK(s)`。

生成的调试 APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。发布版本应使用项目所有者的签名密钥生成。

项目允许明文 HTTP 站点访问，因此仅应添加受信任的内网或自有站点。
