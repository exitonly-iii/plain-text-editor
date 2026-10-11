// ============================================================================
// build.gradle.kts（项目根目录）
// 顶层构建文件：只声明“用哪些插件”，不配置具体内容。
// 插件版本统一写在 gradle/libs.versions.toml 里。
// ============================================================================

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
