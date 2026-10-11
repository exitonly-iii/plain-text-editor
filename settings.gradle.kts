// ============================================================================
// settings.gradle.kts
// Gradle 的“总目录”：声明仓库地址，以及这个项目包含哪些模块。
// 你一般不需要改这个文件。
// ============================================================================

pluginManagement {
    repositories {
        // Android 官方插件（AGP）在 Google 的仓库里
        google()
        // Kotlin 插件等第三方库在 Maven Central
        mavenCentral()
        // Gradle 官方插件门户
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // 只允许在 settings 里声明仓库，模块里的 build.gradle.kts 不再单独声明，避免冲突
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// 项目名称（只影响显示，不影响包名）
rootProject.name = "PlainTextEditor"

// 只有一个模块：app
include(":app")
