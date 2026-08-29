# Android 部署环境指南

本文说明如何为 SiteHubAndroid 准备本地开发环境、配置 GitHub Actions 自动打包，并将 APK 作为 GitHub Release 附件发行。

## 1. 项目要求

项目采用 Android Gradle Plugin `8.6.1`、Kotlin `2.0.21`，编译目标为 Android API 35。

| 项目 | 版本或要求 | 用途 |
| --- | --- | --- |
| JDK | 17 | Gradle 与 Android 构建 |
| Gradle | 8.10.2 | 本地构建；GitHub Actions 已固定此版本 |
| Android SDK Platform | `android-35` | `compileSdk = 35` |
| Android Build Tools | `35.0.0` | APK 打包 |
| Android Studio | 当前稳定版 | 推荐的本地开发与调试工具 |
| GitHub Actions | 已启用 | 云端自动构建与产物保存 |

> 项目当前未提交 Gradle Wrapper。新环境进行命令行构建时，需要自行安装 Gradle `8.10.2`，或先由维护者补充 Gradle Wrapper。

## 2. 获取项目并确认仓库

```bash
git clone https://github.com/miaolik/V2_Elaina-az.git
cd V2_Elaina-az
git status --short --branch
```

预期当前分支为 `main`，远程仓库为 `miaolik/V2_Elaina-az`。

## 3. 本地开发环境

### Android Studio

1. 安装 Android Studio，并在 SDK Manager 中安装以下组件：
   - Android SDK Platform 35
   - Android SDK Build-Tools 35.0.0
   - Android SDK Platform-Tools
2. 通过 `File > Open` 打开项目根目录。
3. 在 Gradle JDK 设置中选择 JDK 17。
4. 等待 Gradle 同步完成。
5. 使用模拟器或 Android 设备启动 `app` 模块。

### 命令行构建

确保 `java -version` 显示 JDK 17，且 `gradle --version` 显示 Gradle 8.10.2。Android SDK 路径可通过 Android Studio 创建的 `local.properties` 配置：

```properties
sdk.dir=/absolute/path/to/Android/Sdk
```

`local.properties` 仅用于本地环境，已被 `.gitignore` 排除。运行以下命令生成调试 APK：

```bash
gradle --no-daemon assembleDebug
```

产物路径：

```text
app/build/outputs/apk/debug/app-debug.apk
```

配置发布签名后可生成发布 APK：

```bash
export ANDROID_KEYSTORE_PATH=/absolute/path/to/release.keystore
export ANDROID_KEYSTORE_PASSWORD=<keystore-password>
export ANDROID_KEY_ALIAS=<key-alias>
export ANDROID_KEY_PASSWORD=<key-password>
gradle --no-daemon assembleRelease
```

发布产物路径：

```text
app/build/outputs/apk/release/app-release.apk
```

请通过安全的凭证管理方式提供签名变量。不要将 keystore、密码、别名或任何令牌提交到仓库。

## 4. GitHub Actions 自动打包

工作流文件位于 `.github/workflows/build-apk.yml`，工作流名称为 `Build Android APK`。

触发方式：

- 推送提交到 `main` 分支。
- 在 GitHub 仓库的 `Actions` 页面手动执行 `Build Android APK`。

云端 Runner 会完成以下步骤：

1. 检出 `main` 分支源码。
2. 配置 Temurin JDK 17。
3. 配置 Gradle 8.10.2。
4. 安装 Android SDK、API 35、Build Tools 35.0.0 并接受 SDK 许可。
5. 检查签名 Secrets 是否完整。
6. 签名信息完整时执行 `assembleRelease`，否则执行 `assembleDebug`。
7. 上传 APK Artifact。

构建结束后，在对应 Actions 运行记录的 `Artifacts` 区域下载：

| 签名状态 | Artifact 名称 | APK 路径 |
| --- | --- | --- |
| 已配置签名 | `SiteHub-release-apk` | `app/build/outputs/apk/release/app-release.apk` |
| 未配置签名 | `SiteHub-debug-apk` | `app/build/outputs/apk/debug/app-debug.apk` |

## 5. 配置发布签名

在 GitHub 仓库页面进入 `Settings > Secrets and variables > Actions`，新增以下 Repository Secrets：

| Secret 名称 | 内容 |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | release keystore 文件的 Base64 编码内容 |
| `ANDROID_KEYSTORE_PASSWORD` | keystore 密码 |
| `ANDROID_KEY_ALIAS` | 签名 Key Alias |
| `ANDROID_KEY_PASSWORD` | 签名 Key 密码 |

本地生成 Base64 文本的示例：

```bash
base64 < release.keystore > release.keystore.base64
```

将 `release.keystore.base64` 中的完整单行内容写入 `ANDROID_KEYSTORE_BASE64`。上传后应从本地安全位置保存或清理该临时文本，不将其纳入 Git。

工作流只有在四个 Secrets 都存在且非空时才会构建 release APK。缺少任一项时会自动降级为 debug APK。

### 签名维护原则

- 所有后续正式版本必须使用同一份 release keystore。
- `app/build.gradle.kts` 中的 `versionCode` 必须持续递增。
- `versionName` 应按发布版本同步更新。
- Android 只允许使用相同签名的 APK 覆盖安装更新。
- 旧版调试签名 APK 与正式签名 APK 之间通常需要卸载后重新安装。

## 6. 标准发布流程

1. 修改功能或界面代码。
2. 更新 `app/build.gradle.kts` 的 `versionCode` 和 `versionName`。
3. 本地执行 `gradle --no-daemon assembleDebug`，或提交后使用 GitHub Actions 验证。
4. 检查改动内容：

```bash
git status --short --branch
git diff --check
git diff
```

5. 提交并推送至 `main`：

```bash
git add <changed-files>
git commit -m "<type>: <change summary>"
git push origin main
```

6. 等待 `Build Android APK` 状态显示 `success`。
7. 下载 Artifact 并安装验证，或创建 GitHub Release 并将 APK 上传为附件。

## 7. 创建 GitHub Release

推荐使用与应用 `versionName` 对应的 tag，例如 `v1.3.6`。GitHub CLI 示例：

```bash
gh release create v1.3.6 app/build/outputs/apk/release/app-release.apk \
  --target <full-commit-sha> \
  --title "V2_Elaina v1.3.6" \
  --notes "- 更新内容一。\n- 更新内容二。"
```

使用 GitHub Actions Artifact 创建 Release 时：

1. 在成功运行页面下载 `SiteHub-release-apk` 或 `SiteHub-debug-apk`。
2. 解压得到 APK。
3. 创建与版本号对应的 tag 和 Release。
4. 将 APK 作为 Release 附件上传，并使用清晰的文件名，例如 `SiteHub-v1.3.6.apk`。
5. 在 Release Notes 中说明功能变化、签名类型和已知限制。

## 8. 常见问题排查

### GitHub Actions 构建为 debug APK

检查四项签名 Secrets 是否全部存在且内容完整。工作流会在 `Detect release signing` 步骤显示是否启用发布签名。

### GitHub Actions 无法安装 Android SDK

确认工作流保留以下步骤：`android-actions/setup-android@v3`、`sdkmanager --licenses`、`platforms;android-35`、`build-tools;35.0.0`。

### 本地命令提示找不到 Gradle

安装 Gradle 8.10.2 并加入 `PATH`，或者在 Android Studio 内完成构建。项目当前未包含 `gradlew` 文件。

### APK 无法覆盖安装

检查 APK 是否使用了与已安装版本相同的签名，且 `versionCode` 大于已安装版本。调试签名与发布签名之间可先卸载旧应用再安装。

### GitHub Release 创建失败，提示 target 无效

使用完整 commit SHA，不使用简短 SHA。例如：

```bash
git rev-parse HEAD
```

将命令输出的完整哈希传递给 `gh release create --target`。

## 9. 安全与维护清单

- 将 keystore、`.jks`、`.keystore`、`local.properties` 保留在 Git 追踪范围外。
- 仅在 GitHub Secrets 或受控凭证系统中存储签名敏感信息。
- 每次发布前确认版本号、分支、提交范围和 APK 签名类型。
- 每次发布后保留对应 Actions 运行链接、Release 链接和 APK 文件名，便于问题回溯。
- 项目允许访问 HTTP 网站，仅添加可信的内网或自有站点。
