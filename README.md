# 纯文本编辑器

[![Build Debug APK](https://github.com/exitonly-iii/plain-text-editor/actions/workflows/build.yml/badge.svg)](https://github.com/exitonly-iii/plain-text-editor/actions/workflows/build.yml)

一个专注、克制的 Android 纯文本编辑器：**不申请任何权限**，所有文件都保存在应用自己的私有目录里。

## 特性

**编辑**

- 基于系统原生文本控件：多行、滚动、选择、复制粘贴
- 打开 / 编辑 / 保存纯文本；有未保存改动时标题显示 `*`、状态栏显示「已修改」，切换文件前会确认
- 字号可调；浅色 / 深色 / 跟随系统

**文件管理**

- 内置文件浏览器：面包屑导航、新建文件夹与文本文件、重命名、删除、复制、移动
- 复制与移动遇到同名项目自动改名（`名字 (2)`），不会覆盖；删除前二次确认

**工作区**

- 侧边栏工作区树，文件夹可逐层展开，点文件直接在编辑区打开
- 工作区只记录路径，不复制文件；在浏览器里添加后侧边栏立即出现

**查找替换**

- 普通查找与正则查找，支持大小写敏感、全词匹配、多行模式
- 单处替换与全部替换；正则替换支持 `$1`、`$2` 捕获组
- 运行在后台线程，有匹配上限与时间预算；正则写错只给提示，不会卡死

**文本格式**

- 自动识别 UTF-8 BOM / UTF-16 / UTF-32，无 BOM 按 UTF-8
- 自动识别 LF / CRLF，保存时保持原格式；也可手动切换

**保护与性能**

- 草稿自动保存（间隔可调），异常退出后重新打开同一文件可恢复
- 大文件（默认超过 5MB，可调）以只读方式**分块**打开：只读可见区域，省内存不卡顿，支持流式查找与跳转

**导入导出**

- 用系统文件选择器导入外部文件（原样复制，不改编码）
- 导出到任意位置；通过系统分享面板分享

## 下载与安装

到 [Releases](https://github.com/exitonly-iii/plain-text-editor/releases/latest) 下载最新的 `app-debug.apk`：

1. 用手机浏览器打开下载页并下载 APK
2. 首次安装需要在系统设置中允许「安装未知应用」
3. 若系统安全策略拦截，请临时关闭对应的拦截项后再安装

> 这是 debug 版安装包，签名固定（仓库内的 `app/debug.keystore`，即 Android 约定的调试密钥，不是机密），因此后续版本可以直接覆盖安装。

## 权限与隐私

- **不申请任何权限**：`AndroidManifest.xml` 中没有任何 `uses-permission`
- 文件只读写应用私有目录（`/data/data/com.example.plaintexteditor/files`），其它应用无法访问，卸载即删除
- 导入 / 导出使用系统文件选择器（SAF），由系统按次授权，不需要存储权限
- 分享通过 FileProvider，只暴露你主动分享的那一个文件
- 没有网络权限：不联网、无账号、无广告、无统计

## 构建

### 用 GitHub Actions（零本地环境）

推送到 `main` 分支会自动构建；也可以在 **Actions → Build Debug APK → Run workflow** 手动触发。构建完成后在运行页面底部下载 `app-debug-apk`，解压即为 APK。

### 本地构建

需要 JDK 17+ 与 Android SDK（Android Studio 自带），Gradle 使用 8.13：

```bash
gradle assembleDebug
```

产物在 `app/build/outputs/apk/debug/app-debug.apk`。

> 仓库内不放 `gradle-wrapper.jar`。需要 wrapper 的话，在项目根目录执行一次 `gradle wrapper --gradle-version 8.13` 生成 `gradlew` 与 jar 即可。

## 技术栈

| 组件 | 版本 |
| --- | --- |
| Kotlin | 2.1.21 |
| Android Gradle Plugin | 8.11.1 |
| Gradle | 8.13 |
| Jetpack Compose BOM | 2024.12.01 |
| DataStore Preferences | 1.2.1 |
| minSdk / targetSdk / compileSdk | 26 / 36 / 36 |

架构：单 Activity + Jetpack Compose。`data/` 是数据层（文件读写、设置、草稿、查找、文本格式），界面层只通过仓库访问数据，所有 IO 都在协程里执行。

## 项目结构

```
app/src/main/java/com/example/plaintexteditor/
├── PlainTextEditorApp.kt      应用入口，提供全局唯一的仓库实例
├── MainActivity.kt            主界面 Activity
├── data/
│   ├── model/                 数据模型（文件项、工作区项）
│   ├── repository/            FileRepository：全部文件读写
│   ├── workspace/             工作区列表（DataStore）
│   ├── settings/              应用设置（DataStore）
│   ├── draft/                 草稿存储
│   ├── search/                查找替换与正则
│   ├── text/                  编码 / 换行符 / 行号 / 大文件分块索引
│   └── state/                 界面状态盒子
└── ui/
    ├── editor/                主界面、编辑区、顶部栏、状态栏、查找面板、工作区树、大文件只读视图
    ├── browser/               文件浏览器
    ├── settings/              设置页
    ├── common/                公共弹窗、底部菜单行、导入导出分享工具
    └── theme/                 Material 3 主题
```

## 已知限制

- 大文件只读模式按每 200 行分块加载，滚动为分块推进而非逐行平滑；跨块边界的匹配（例如让正则 `.` 跨行）可能漏掉，普通查找不受影响
- 大文件只读模式下不支持替换、不检查草稿、不支持切换编码
- 行号只显示逻辑行（段号）与当前行；没有常驻行号列，也不做列号
- 草稿位于缓存目录，系统空间紧张时可能被清理；正式内容始终以已保存的文件为准
- 没有语法高亮、Markdown 预览、云同步

## 常见问题

**安装时提示「解析包时出现错误」**

多数情况下是安装的文件不对：确认文件以 `.apk` 结尾、大小约 9–11 MB，并且不在 `/Android/data/...` 这类应用私有目录里（系统安装器读不到那里的文件）。用手机浏览器直接从 Releases 下载最省事。

**升级安装提示「应用未安装 / 签名不一致」**

0.1.x 的安装包使用构建时临时生成的调试密钥，签名与后续版本不同；从 0.2.0 起签名已固定，可直接覆盖安装。遇到这种情况先卸载旧版本再安装新版本。

**构建失败提示 `Failed to find package 'tools'`**

这是旧版 `android-actions/setup-android@v3` 的问题，本项目已改用 `@v4`。

## 许可证

本项目采用 [MIT 许可证](LICENSE)。所依赖的开源库（Jetpack Compose、AndroidX、DataStore、Kotlin）均为 Apache-2.0，与 MIT 兼容。
