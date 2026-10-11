# 纯文本编辑器（Android）

一个用 **Kotlin + Jetpack Compose + Material 3** 写的第一版纯文本编辑器骨架。

- 应用名称：纯文本编辑器
- 包名：`com.example.plaintexteditor`
- 最低支持：Android 8.0（API 26）；目标版本：Android 16（API 36）
- 不申请任何权限；文件只会放在应用自己的私有目录里

**第一阶段只搭好界面骨架**：顶部栏、侧边栏、编辑区占位、底部状态栏。
顶部栏的“保存 / 查找 / 更多”、侧边栏的“浏览私有目录 / 设置”**点了不会有任何反应**，这是刻意留的占位，第二阶段才会接上真正的功能。

---

## 一、每个文件放在哪里

下面是完整文件树。你不需要记住它，只要在电脑上建出**一模一样的文件夹和文件名**即可。
（`app/src/main/java/...` 这种很长的路径是 Android 项目的标准结构，照抄就行。）

```
plain-text-editor/                        ← 项目根目录（名字可以自己取）
├── .github/                              ← 开头有个点，Windows 里默认隐藏
│   └── workflows/
│       └── build.yml                     ← 云端自动构建 APK 的脚本
├── .gitignore                            ← 告诉 Git 哪些文件不要上传
├── README.md                             ← 你正在看的这个文件
├── build.gradle.kts                      ← 根构建文件（声明插件）
├── settings.gradle.kts                   ← 声明仓库地址和模块
├── gradle.properties                     ← 构建的全局开关
├── gradle/
│   ├── libs.versions.toml                ← 所有版本号集中在这里
│   └── wrapper/
│       └── gradle-wrapper.properties     ← 声明使用 Gradle 8.13
└── app/                                  ← 应用模块（真正装到手机上的东西）
    ├── build.gradle.kts                  ← 包名 / SDK 版本 / 依赖库
    ├── proguard-rules.pro                ← 混淆规则（暂时是空的）
    └── src/
        └── main/
            ├── AndroidManifest.xml       ← 应用说明书（名称、图标、入口）
            ├── java/com/example/plaintexteditor/
            │   ├── MainActivity.kt                       ← 应用入口
            │   └── ui/
            │       ├── theme/
            │       │   ├── Color.kt                      ← 浅色/深色配色
            │       │   ├── Theme.kt                      ← 主题（含浅色/深色/跟随系统）
            │       │   └── Type.kt                       ← 字体样式
            │       └── editor/
            │           ├── EditorScreen.kt               ← 主界面总装
            │           ├── EditorTopBar.kt               ← 顶部栏
            │           ├── WorkspaceDrawer.kt            ← 侧边栏（工作区）
            │           ├── EditorStatusBar.kt            ← 底部状态栏
            │           ├── NoFileOpenedPlaceholder.kt    ← 编辑区“未打开文件”占位
            │           └── PlainTextEditor.kt            ← 编辑区（EditText 封装）
            └── res/
                ├── drawable/
                │   └── ic_launcher_foreground.xml        ← 图标的图案（矢量图）
                ├── mipmap-anydpi-v26/
                │   ├── ic_launcher.xml                   ← 图标
                │   └── ic_launcher_round.xml             ← 圆形图标
                ├── values/
                │   ├── colors.xml                        ← 图标背景色、窗口背景色
                │   ├── strings.xml                       ← 界面上所有文字
                │   └── themes.xml                        ← 窗口外观（浅色）
                ├── values-night/
                │   ├── colors.xml                        ← 深色模式下的颜色
                │   └── themes.xml                        ← 窗口外观（深色）
                └── xml/
                    ├── backup_rules.xml                  ← 不备份任何内容
                    └── data_extraction_rules.xml         ← 不备份、不迁移
```

要改的东西基本只有三处：

| 想改什么 | 改哪个文件 |
| --- | --- |
| 界面上的文字（标题、按钮文字） | `app/src/main/res/values/strings.xml` |
| 库的版本号 | `gradle/libs.versions.toml` |
| 应用名 / 版本号 / 最低支持的安卓版本 | `app/build.gradle.kts` |

---

## 二、三步拿到能装在手机上的 APK（推荐，不用装任何开发软件）

编译在 GitHub 的服务器上完成，你的电脑什么都不用装。

### 第 1 步：把项目上传到 GitHub

1. 打开 <https://github.com/new>
2. **Repository name** 填 `plain-text-editor`；Public / Private 都行；
   **不要**勾选 “Add a README file”；点 **Create repository**
3. 在新仓库页面点 **uploading an existing file** 链接
4. 在电脑上打开项目文件夹，选中并拖进网页：
   - `app` 文件夹
   - `gradle` 文件夹
   - `.github` 文件夹 ← **最容易漏掉的一个，漏了就没有自动构建**
   - `.gitignore`
   - `README.md`
   - `build.gradle.kts`
   - `settings.gradle.kts`
   - `gradle.properties`

   > Windows 的资源管理器默认不显示以点开头的文件/文件夹。
   > 在文件夹窗口点「查看」→ 勾选「隐藏的项目」，就能看到 `.github` 和 `.gitignore`。
5. 页面拉到最下面点 **Commit changes**

> **如果 `.github` 总是传不上去**，用下面这个更稳的办法手动建：
> 仓库页面 → **Add file** → **Create new file** → 文件名框里输入
> `.github/workflows/build.yml`（输入斜杠时会自动变成多层文件夹）
> → 把本仓库里 `.github/workflows/build.yml` 的内容原样粘进去 → **Commit changes**。

### 第 2 步：让云端自动构建

上传完成（也就是第 1 步的 Commit）之后，构建会**自动开始**：

1. 打开仓库页面顶部的 **Actions** 标签
2. 左侧会看到 **Build Debug APK**，点进去
3. 看到一条正在转圈/打勾的运行记录。第一次构建大约 **5～10 分钟**（云端要下载 Gradle 和 Android SDK）
4. 出现绿色 ✅ 表示成功；红色 ❌ 表示失败，点进去看日志（见文末「常见问题」）

### 第 3 步：下载并安装到手机

1. 在刚才那条成功的运行记录页面，往下拉到最底部的 **Artifacts** 区域
2. 点 **app-debug-apk** 下载（得到的是一个 `.zip` 文件）
3. 解压这个小压缩包，里面就是 **app-debug.apk**（这就是安装包）
4. 把 `app-debug.apk` 传到手机：数据线拷贝、微信「文件传输助手」、网盘等都可以
5. 在手机上点开这个文件安装

**荣耀 MagicOS（Android 16）安装提示：**

- 第一次安装会提示「未知来源 / 外部来源」，需要允许：设置 → 安全 → 更多安全设置 → 安装未知应用（不同系统版本文字略有差别）
- 如果被**「纯净模式」**拦截、提示应用未经检测：设置 → 安全 → 纯净模式 → 暂时关闭，装完可以再打开
- 安装完成后桌面出现「纯文本编辑器」，打开后能看到的界面是：顶部「纯文本编辑器」标题栏、左下角菜单按钮滑出的「工作区」侧边栏、中间「未打开文件」、底部「UTF-8 / LF / 行数 -- / 行号 --」

> GitHub 上构建好的压缩包默认只保留 90 天，过期后重新跑一次构建即可。

---

## 三、手动重新构建

除了「上传代码自动构建」，你也可以随时手动点一次：

**Actions** 标签 → 左侧选 **Build Debug APK** → 右侧 **Run workflow** 按钮 → 选 `main` 分支 → **Run workflow**

> 如果你的仓库默认分支叫 `master` 而不是 `main`，推送代码不会触发自动构建。
> 解决办法二选一：把分支改名为 main（仓库 Settings → Branches），
> 或者把 `.github/workflows/build.yml` 里的 `branches: [ "main" ]` 改成 `[ "master" ]`。

---

## 四、想在电脑上改代码（可选）

需要两样东西：

1. **Android Studio**（免费）：<https://developer.android.com/studio>
2. **Gradle 8.13**：<https://gradle.org/releases/>（下载 `gradle-8.13-bin.zip`，解压后把 `bin` 目录加到系统 PATH）

**为什么仓库里没有 `gradlew` 和 `gradle-wrapper.jar`？**
`gradle-wrapper.jar` 是一个二进制文件，按你的要求没有放进项目（GitHub Actions 直接用 `gradle` 命令构建，不需要它）。
想在本地用 Wrapper 的话，在项目目录里执行一次：

```bash
gradle wrapper --gradle-version 8.13
```

它会自动生成 `gradlew`、`gradlew.bat` 和 `gradle-wrapper.jar`，之后就可以用 `./gradlew assembleDebug` 了，Android Studio 也能直接打开这个项目。

本地构建命令（在项目根目录执行）：

```bash
gradle assembleDebug
```

生成的 APK 在 `app/build/outputs/apk/debug/app-debug.apk`。
注意：本机需要 Android SDK（Android Studio 会帮你装），并且**不要**把 `local.properties` 上传到 GitHub（`.gitignore` 已经帮你排除了）。

---

## 五、版本选择说明

所有版本号都在 `gradle/libs.versions.toml` 里，改一处就能整体升级。

| 组件 | 版本 | 为什么选它 |
| --- | --- | --- |
| Gradle | 8.13 | Android Gradle Plugin 8.11 要求的最低 Gradle 版本，稳定且下载量最大 |
| Android Gradle Plugin | 8.11.1 | 完整支持 `compileSdk 36`（Android 16）；更早的 8.9/8.10 编译 API 36 会提示“未经测试” |
| Kotlin | 2.1.21 | 与 AGP 8.11、Compose 编译器插件配套的稳定版本 |
| Compose BOM | 2024.12.01 | 一套经过验证的 Compose 稳定版本，Material 3 组件齐全（含 `ModalNavigationDrawer`、`CenterAlignedTopAppBar` 等本界面用到的控件） |
| JDK（云端） | 17 | AGP 8.11 官方支持；Android Studio 自带的 JDK 也能用 |

这组版本偏“稳”而不是“最新”，原因是**我无法在离线环境中联网核对最新版本号**，所以选了确定存在、且互相兼容的一组。
想升级时：打开 <https://developer.android.com/build/releases/gradle-plugin> 看最新 AGP 与它要求的 Gradle 版本，改 `libs.versions.toml` 里对应的数字即可。

---

## 六、常见问题

**构建失败，日志里有 `Could not find ...`（某个库的版本找不到）**
说明这个版本号在 Maven 仓库里不存在（版本号写错或已被移除）。打开 `gradle/libs.versions.toml`，
把对应的那行换成一个确定存在的版本号，提交后重新构建。例如：

```toml
# 报错说找不到 AGP 8.11.1 时
agp = "8.12.0"

# 报错说找不到 compose-bom 2024.12.01 时
composeBom = "2025.01.00"
```

可用的 AGP / Gradle 组合查这里：<https://developer.android.com/build/releases/gradle-plugin>；
可用的 Compose BOM 版本查这里：<https://developer.android.com/develop/ui/compose/bom/bom-mapping>。

**构建失败，日志里有 `gradle: command not found`**
说明云端没把 `gradle` 加进 PATH。在 `.github/workflows/build.yml` 的 `Build debug APK` 步骤**之前**插入一个步骤手动下载：

```yaml
      - name: Install Gradle manually
        run: |
          curl -sSL -o /tmp/gradle.zip https://services.gradle.org/distributions/gradle-8.13-bin.zip
          unzip -q /tmp/gradle.zip -d /opt
          echo "/opt/gradle-8.13/bin" >> $GITHUB_PATH
```

**构建失败，日志里有 `SDK location not found` 或许可证相关报错**
工作流里已经包含「接受许可证」和「写入 local.properties」两个步骤；若是偶发网络问题，点 **Re-run all jobs** 重跑一次通常就好。

**构建失败，日志里有 `Failed to find package 'tools'`**
这是 `android-actions/setup-android@v3` 的问题：它默认会安装 `tools` 包，而 Google 已把它从 SDK 仓库移除，
所以这一步会直接报错。本项目已经改用 `android-actions/setup-android@v4`（v4 的默认包只有 `platform-tools`）。
如果你在别处复制到旧的 `@v3` 写法，把版本号改成 `@v4` 即可。

**界面能装能开，但点按钮没反应**
这是第一阶段的预期行为：保存、查找、更多、浏览私有目录、设置都还是占位。

**想改应用名、图标颜色**
应用名改 `app/src/main/res/values/strings.xml` 里的 `app_name`；
图标背景色改 `app/src/main/res/values/colors.xml` 里的 `ic_launcher_background`。

---

## 七、第二阶段可以接的功能

代码里已经留好了这些“接口位”，接起来会很自然：

- `PlainTextEditor` 已经是可用的编辑控件，把文件内容传进 `text` 就会显示、编辑
- `EditorScreen` 里的 `isFileOpen` 改成 `true` 就从占位切到真正的编辑区
- 私有目录读写用 `context.filesDir`，**不需要任何权限**
- `ThemeMode` 已经支持浅色/深色/跟随系统，只差在设置页里做一个切换开关
