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
    ├── debug.keystore                    ← 固定的调试签名钥匙（保证新版能覆盖安装旧版）
    ├── proguard-rules.pro                ← 混淆规则（暂时是空的）
    └── src/
        └── main/
            ├── AndroidManifest.xml       ← 应用说明书（名称、图标、入口）
            ├── java/com/example/plaintexteditor/
            │   ├── PlainTextEditorApp.kt                 ← 应用入口（提供全应用唯一的仓库实例）
            │   ├── MainActivity.kt                       ← 主界面 Activity
            │   ├── data/                                 ← 数据层（第二阶段新增，没有界面代码）
            │   │   ├── model/
            │   │   │   ├── FileItem.kt                   ← 一个文件/文件夹的信息
            │   │   │   └── WorkspaceItem.kt              ← 一个工作区条目
            │   │   ├── repository/
            │   │   │   └── FileRepository.kt             ← 所有文件读写都在这里
            │   │   ├── workspace/
            │   │   │   └── WorkspaceStore.kt             ← 工作区列表的保存/读取（DataStore）
            │   │   └── state/
            │   │       ├── FileBrowserState.kt           ← “文件列表”状态盒子
            │   │       └── WorkspaceState.kt             ← “工作区列表”状态盒子
            │   └── ui/
            │       ├── browser/                          ← 文件浏览器页面（第三阶段新增）
            │       │   ├── FileBrowserScreen.kt          ← 页面主体（面包屑 + 列表 + 弹窗）
            │       │   ├── FileEntryRow.kt               ← 列表里的一行
            │       │   ├── FileActionSheet.kt            ← 底部操作菜单 / 新建 / 选择目标文件夹
            │       │   └── FileBrowserFormatters.kt      ← 大小 / 时间 / 路径的显示格式
            │       ├── common/                           ← 两个页面共用的界面零件
            │       │   ├── FileActionDialogs.kt          ← 输入名字、删除确认
            │       │   └── ActionSheetRow.kt             ← 底部菜单里的一行
            │       ├── theme/
            │       │   ├── Color.kt                      ← 浅色/深色配色
            │       │   ├── Theme.kt                      ← 主题（含浅色/深色/跟随系统）
            │       │   └── Type.kt                       ← 字体样式
            │       └── editor/
            │           ├── EditorScreen.kt               ← 主界面总装
            │           ├── WorkspaceTree.kt              ← 侧边栏工作区文件树（第四阶段新增）
            │           ├── EditorTopBar.kt               ← 顶部栏
            │           ├── WorkspaceDrawer.kt            ← 侧边栏（工作区）
            │           ├── EditorStatusBar.kt            ← 底部状态栏
            │           ├── NoFileOpenedPlaceholder.kt    ← 编辑区“未打开文件”占位
            │           └── PlainTextEditor.kt            ← 编辑区（EditText 封装）
            └── res/
                ├── drawable/
                │   ├── ic_file.xml                       ← 文件图标（矢量图）
                │   ├── ic_folder.xml                     ← 文件夹图标（矢量图）
                │   └── ic_launcher_foreground.xml        ← 启动图标的图案（矢量图）
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

**手机上安装时提示“解析包时出现错误”**
这不是构建的问题（APK 已通过官方 `apksigner` 与 `zipalign` 校验）。请按顺序排查：

1. 文件名必须以 `.apk` 结尾。GitHub Actions 里的 Artifacts 下载下来是**压缩包** `app-debug-apk.zip`，
   必须先解压；另外有些传输类 App 会改写扩展名（例如变成 `app-debug.apk.1`），请用手机文件管理器看清完整名字。
2. 文件大小约 **9.5 MB（9,500,000 字节左右，每次构建略有差异）**。如果明显小很多，说明文件不完整。
3. 文件不要放在 `/Android/data/...` 这类**应用私有目录**里——系统的安装器读不到，同样会报这个错。
   用文件管理器把它移动或复制到「下载 / Download」目录，再点它安装。
4. 先卸载手机上可能已经存在的旧「纯文本编辑器」，签名不一致也会装不上。
5. 最省事：直接下载仓库 **Releases** 页面里挂好的 APK（那是 APK 本体，不用解压）。

**升级安装时提示“应用未安装 / 签名不一致”**
v0.1.x 用的是 GitHub Actions 每次临时生成的调试密钥，所以那几版的签名互不相同。
从 v0.2.0 开始，项目里自带了固定的调试密钥（`app/debug.keystore`），以后每一版都能直接覆盖安装。
从 v0.1.1 升到 v0.2.0 这一次，请先卸载手机上的旧「纯文本编辑器」，再安装新版（只有这一次需要）。

**界面能装能开，但点按钮没反应**
这是第一阶段的预期行为：保存、查找、更多、浏览私有目录、设置都还是占位。

**想改应用名、图标颜色**
应用名改 `app/src/main/res/values/strings.xml` 里的 `app_name`；
图标背景色改 `app/src/main/res/values/colors.xml` 里的 `ic_launcher_background`。

---

## 七、文件系统数据层（第二阶段已完成）

数据层没有任何界面代码，只负责“文件怎么读写、工作区怎么记住”。

### 路径约定
所有 path 都是**相对应用私有根目录**的相对路径，`""` 表示根目录本身：

| 例子 | 含义 |
| --- | --- |
| `""` | 私有根目录（`filesDir`） |
| `"notes"` | 根目录下的 notes 文件夹 |
| `"notes/日记.txt"` | notes 文件夹里的 日记.txt |

为什么要用相对路径：应用被“分身”、数据目录被系统换位置时，绝对路径会失效，相对路径不会。
需要绝对路径时用 `FileRepository.rootAbsolutePath`（只用于显示，不要保存）。

### 三个仓库 / 状态对象

| 文件 | 作用 | 用法 |
| --- | --- | --- |
| `FileRepository` | 所有文件读写 | 每个函数都是 `suspend`，返回 `Result`；成功 `getOrNull()`，失败 `exceptionOrNull()?.message` |
| `WorkspaceStore` | 工作区列表 | `workspaces` 是 `Flow`，界面 `collect` 它就会自动刷新 |
| `FileBrowserState` / `WorkspaceState` | 状态盒子 | 界面拿它们来存“此刻的样子” |

`FileRepository` 提供的操作：`list` 列目录、`createDirectory`、`createTextFile`、`rename`、`delete`、
`copy`、`move`、`readText`、`writeText`、`getItem`、`exists`。
全部在后台线程执行，且都被限制在私有目录内（`../` 之类的路径会被拒绝）。

---

## 八、文件浏览器页面（第三阶段已完成）

入口：主界面左上角菜单 → 侧边栏底部「浏览私有目录」→ 全屏打开文件浏览器。

能做到的事：

- 从私有目录根开始，一层一层进入文件夹；顶部面包屑（私有目录 › notes › 2026）点哪一层就跳到哪一层
- 文件夹排在前面、文件排在后面，文件显示大小和修改时间
- 顶部「+」→ 新建文件夹 / 新建文本文件
- 点文件夹进入；点文件直接回到主界面，并在编辑区打开它
- 长按某一行（或点它右边的「⋮」）→ 添加到工作区 / 重命名 / 复制到… / 移动到… / 删除
- 复制和移动会弹出“选择目标文件夹”的面板；遇到同名项目自动改名成「名字 (2)」，不会覆盖任何东西
- 删除会二次确认；操作结果用底部提示条告知
- 系统返回键：在子目录里是返回上一级，在根目录是关闭浏览器

相关文件：

| 文件 | 作用 |
| --- | --- |
| `ui/browser/FileBrowserScreen.kt` | 页面主体：顶部栏、面包屑、列表、各种弹窗的组装 |
| `ui/browser/FileEntryRow.kt` | 列表里的一行（图标 / 名字 / 大小时间 / 更多按钮） |
| `ui/browser/FileActionSheet.kt` | 底部操作菜单、新建菜单、选择目标文件夹 |
| `ui/common/FileActionDialogs.kt` | 输入名字的弹窗、删除确认弹窗（两个页面共用） |
| `ui/browser/FileBrowserFormatters.kt` | 大小 / 时间 / 路径的显示格式 |
| `res/drawable/ic_folder.xml`、`ic_file.xml` | 文件夹和文件的图标（矢量图，不需要图片文件） |

### 后续阶段要做的

- 编辑区接文件：`PlainTextEditor` 已经是可用控件，把 `readText` 的内容传进去即可
- 设置页里的浅色/深色/跟随系统开关（`ThemeMode` 已支持三种）

---

## 九、侧边栏工作区文件树（第四阶段已完成）

侧边栏现在就是“工作区资源管理器”，逻辑参考 VS Code，但为手机触摸做了简化。

能做到的事：

- 顶部标题「工作区」，下面就是阶段二保存下来的工作区列表（存在 DataStore 里）
- 文件夹工作区可以一层层展开/收起（每层缩进 16dp，整行高度 ≥ 48dp，手指点得准）
- 文件工作区直接显示成一个可点的条目
- **点文件**：自动收起侧边栏，并在编辑区打开这个文件（内容读写见第十节）
- **长按任意一行**：弹出菜单 → 从工作区移除（仅当这一行本身就是工作区）/ 重命名 / 删除
- 工作区为空时，中间显示「暂无工作区，点击下方浏览私有目录添加」
- 底部仍是「浏览私有目录」按钮（打开第三阶段的文件浏览器）和设置入口（占位）

同步关系：工作区列表来自 DataStore 的 `Flow`，所以在文件浏览器里“添加到工作区”之后，
侧边栏会立刻出现；从浏览器返回、或在树里做了重命名/删除，树会自动重新读一遍磁盘。

相关文件：

| 文件 | 作用 |
| --- | --- |
| `ui/editor/WorkspaceTree.kt` | 工作区文件树：递归展开、长按菜单、重命名/删除弹窗 |
| `ui/editor/WorkspaceDrawer.kt` | 侧边栏外壳：标题、文件树、底部按钮 |
| `ui/editor/EditorScreen.kt` | 主界面：打开/保存文件、未保存提醒、提示条与刷新 |
| `ui/editor/NoFileOpenedPlaceholder.kt` | 编辑区占位（还没打开文件时显示“未打开文件”） |
| `ui/common/ActionSheetRow.kt` | 底部菜单里的一行（两个页面共用） |

---

## 十、编辑区读写文件（第五阶段已完成）

这一阶段编辑区真正能用了：打开文件、改内容、保存回原文件。

能做到的事：

- 从侧边栏工作区文件树、或文件浏览器点一个文件 → 编辑区加载它的内容（读取过程中显示“正在打开文件…”）
- 打开失败（文件被删了、读不了）→ 底部提示条说明原因，不会切走当前文件
- 顶部栏中间显示当前文件名；有未保存修改时显示成「文件名 *」，此时保存按钮才可以点
- 点「保存」→ 写回原文件，成功后提示「已保存」，标题上的 `*` 消失
- 底部状态栏：有未保存修改时最左边显示「已修改」，后面依次是 UTF-8 / LF / 行数占位 / 行号占位
- 切换文件时的保护：当前文件有未保存修改时，会弹出「还没保存」对话框，可选
  「保存」（先保存再打开新文件）或「放弃修改」（直接打开新文件）
- 编辑控件仍是 `AndroidView` 包着系统的 `EditText`：支持多行、滚动、选择、复制粘贴；
  字体默认用系统默认字体（想换成等宽字体，给 `PlainTextEditor` 传 `monospace = true`）

相关文件：

| 文件 | 作用 |
| --- | --- |
| `ui/editor/EditorScreen.kt` | 编辑区状态与流程：打开 / 保存 / 未保存提醒 |
| `ui/editor/PlainTextEditor.kt` | AndroidView + EditText 编辑控件（新增 `monospace` 开关） |
| `ui/editor/EditorTopBar.kt` | 顶部栏：文件名 + 未保存标记，控制保存按钮可用状态 |
| `ui/editor/EditorStatusBar.kt` | 底部状态栏：已修改标记 / UTF-8 / LF / 行数 / 行号 |
| `ui/browser/FileBrowserScreen.kt` | 浏览器里点文件也会交给编辑区打开 |

### 后续阶段可以做的

- 设置页里的浅色/深色/跟随系统开关（`ThemeMode` 已支持三种）

---

## 十一、编码 / 换行符 / 草稿恢复（第六阶段已完成）

打开文件时会先看它的“格式”，保存时按原格式写回去，编辑期间还会偷偷存草稿保命。

能做到的事：

- **编码识别**：先看文件开头有没有 BOM —— UTF-8 BOM、UTF-16（LE/BE）、UTF-32（LE/BE）都能认出来；
  没有 BOM 就一律按 UTF-8 处理（绝大多数纯文本都是这种）
- **换行符识别**：看先出现的是 `\r\n` 还是 `\n`，得出 CRLF 或 LF
- **保存时保持原样**：编码、BOM、换行符都不会被改掉
- **手动切换**：顶部栏「⋮」→「切换编码…」/「切换换行符…」；
  切编码会按新编码重新读一遍文件（有未保存修改时会先提醒你先处理），切换行符会立刻转换内存里的内容
- **状态栏**：显示真实编码（UTF-8 / UTF-16 LE …）和换行符（LF / CRLF），左边是「已修改」标记
- **草稿恢复**：编辑中每 10 秒把内容写进私有缓存目录 `cacheDir/drafts`（文件名是原路径的 SHA-256，不会重名）；
  下次打开同一个文件时，如果发现草稿和文件内容不一样，就问「要恢复吗？」；
  选「恢复」就把草稿内容放回编辑区（记得再点保存），选「丢弃」就删掉草稿
- **保存或放弃后清理草稿**：保存成功、选“放弃修改”、放弃并退出，都会把对应草稿删掉
- **退出保护**：有未保存修改时按系统返回键，会弹出「要退出吗」，可选「保存」/「放弃并退出」（按取消就是不退出）

相关文件：

| 文件 | 作用 |
| --- | --- |
| `data/text/TextFormat.kt` | 编码与换行符的识别 / 解码 / 编码（纯计算，不碰文件） |
| `data/draft/DraftStore.kt` | 草稿的读写删（文件名 = 原路径的 SHA-256） |
| `data/repository/FileRepository.kt` | 新增 `readTextFile` / `writeTextFile`（带格式的读写） |
| `ui/editor/EditorScreen.kt` | 编码/换行/草稿/退出提示的全部流程 |
| `ui/editor/EditorTopBar.kt` | 「⋮」菜单：切换编码 / 切换换行符 |
| `ui/editor/EditorStatusBar.kt` | 显示真实的编码与换行符 |

### 后续阶段可以做的

- 状态栏的“行数 / 行号”换成真实数据
- 大文件只读模式
- 设置页里的浅色/深色/跟随系统开关

---

## 十二、查找替换（第七阶段已完成）

点顶部栏的放大镜按钮，编辑区下方会滑出「查找替换」面板（键盘弹出时它会被顶上去，不会被挡住）。

能做到的事：

- 查找：输入即查（停顿 300 毫秒后开始算），上一个 / 下一个循环跳转，命中处会在编辑区里选中并滚动到可见位置
- 查找选项：普通查找、正则、大小写（默认忽略大小写）、全词、多行（只在正则时可用，其余时候是灰的）
- 替换：单处替换换掉当前这一处；全部替换会先弹确认框，完成后提示「已替换 N 处」
- 正则替换支持捕获组：替换框里写 $1、$2 就能引用分组；普通替换里的 $ 按普通字符处理
- 匹配数量：面板上显示「第 3/12 个匹配」；没有匹配显示「没有匹配」；正则写错显示「正则表达式有语法错误：…」，不会卡死也不会崩
- 后台线程与超时保护：查找替换都在后台线程执行；最多记录 5000 个匹配，单次操作最多 2000 毫秒（界面最多等 3 秒），超时提示「匹配太多或表达式太复杂，结果可能不完整」；全部替换若中途超时，原内容一点都不会改，绝不写半成品
- 只读接口：面板已有 readOnly 开关（只读时替换按钮变灰并提示），留给下一阶段的大文件模式使用

相关文件：

| 文件 | 作用 |
| --- | --- |
| `data/search/TextSearch.kt` | 查找与替换的纯计算实现（正则、捕获组、时间预算、匹配上限） |
| `ui/editor/FindReplacePanel.kt` | 底部查找替换面板（输入框、按钮、选项 chip、匹配计数） |
| `ui/editor/PlainTextEditor.kt` | 新增 SelectionRequest：让上层能把光标/选区放到指定位置 |
| `ui/editor/EditorScreen.kt` | 面板开关、查找状态、跳转与替换流程 |

### 后续阶段可以做的

- 大文件只读模式（面板的只读接口已经预留）
- 设置页里的浅色/深色/跟随系统开关

---

## 十三、轻量行号与状态栏（第八阶段已完成）

状态栏这一排现在是这样：已修改标记 / 编码 / 换行符 / 总行数约数 / 当前光标行号。

能做到的事：

- 总行数：文件内容变化后（停顿 400 毫秒）在后台线程数一次，显示成「约 2340 行」这样的约数
- 当前行号：每 2 秒在后台算一次「光标在第几行」；只有行号真的变了，才会在编辑区右下角弹出一个小角标，显示 1 秒后淡出
- 光标没动就不会反复触发动画（行号不变就不更新）
- 行号按逻辑行算（也就是“段号”）：编辑器自动换行不会让行号虚增
- 开关：顶部栏「⋮」→「显示当前行号」（默认开启，开启时菜单里打勾）；关掉后状态栏不再显示行号，角标也不再出现
- 性能：行号计算全部丢在后台线程（Dispatchers.Default），淡出用的是最轻的 alpha 动画；开关状态存在私有 DataStore 里，重启应用仍然生效

相关文件：

| 文件 | 作用 |
| --- | --- |
| `data/text/LineCounter.kt` | 行号计算（总行数、光标所在行） |
| `data/settings/SettingsStore.kt` | 设置存储（当前行号开关；大文件阈值也放在这里，第九阶段用） |
| `ui/editor/PlainTextEditor.kt` | 新增 `onCursorOffsetChanged`：光标移动时把偏移量告诉上层 |
| `ui/editor/EditorStatusBar.kt` | 状态栏显示总行数与当前行号 |
| `ui/editor/EditorTopBar.kt` | 「⋮」菜单里新增「显示当前行号」开关（带勾选） |
| `ui/editor/EditorScreen.kt` | 2 秒一次的定时器、行号角标与淡出、设置读写 |

### 后续阶段可以做的

- 大文件只读模式（5MB 阈值已可在设置里调整，面板的只读接口也已预留）
- 设置页里的浅色/深色/跟随系统开关
