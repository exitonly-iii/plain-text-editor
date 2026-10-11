// ============================================================================
// app/build.gradle.kts
// app 模块的构建配置：包名、SDK 版本、编译开关、依赖库。
// 这是你最可能改动的文件之一。
// ============================================================================

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    // 代码所在的包（也是 Java/Kotlin 源码的目录结构）
    namespace = "com.example.plaintexteditor"

    // 用哪个版本的 Android SDK 来编译（36 = Android 16）
    compileSdk = 36

    defaultConfig {
        // 安装到手机上之后的唯一标识
        applicationId = "com.example.plaintexteditor"

        // 最低支持 Android 8.0（API 26）
        minSdk = 26

        // 目标版本：Android 16（API 36）
        targetSdk = 36

        // 应用版本号（每次发新版：versionCode 加 1，versionName 可以写 0.2.0）
        versionCode = 8
        versionName = "0.8.0"
    }

    signingConfigs {
        // 这是 debug 包的“签名钥匙”。默认情况下 Android Studio 和 CI 每次都会新生成一把，
        // 于是两次构建的 APK 签名不一致，新版就没法覆盖安装旧版（会提示“应用未安装/签名不一致”）。
        // 这里改成使用项目里自带的那把固定钥匙（app/debug.keystore），
        // 以后每次构建的 APK 都能直接覆盖安装到上一版上。
        // 注意：debug keystore 只是开发用的，不是机密；但也不能用来发布正式版本。
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"

            // 我们的 minSdk = 26，Android Gradle Plugin 默认只给 debug 包做 v2 签名
            // （v1 只在 minSdk < 24 时才默认开启，v3 只在 minSdk >= 28 时才默认开启）。
            // 但部分国产 ROM 的安装器 / 安全检测只认传统的 JAR（v1）签名，
            // 遇到“只有 v2”的包会直接报“解析包时出现错误”。
            // 这里显式把 v1 / v2 / v3 三种签名全部打开，兼容性最好。
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        // debug 用默认配置即可：不混淆，可以直接装到手机上测试
        release {
            // 第一阶段还没有发布计划，先不开启代码混淆
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    // Java 版本：17（Android Studio 自带的 JDK 和 GitHub Actions 的 JDK 17 都支持）
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        // 打开 Jetpack Compose
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        // Kotlin 编译出来的字节码版本，和上面的 Java 17 保持一致
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // AndroidX 基础库
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)

    // Activity + Compose 的桥梁（setContent、enableEdgeToEdge）
    implementation(libs.activity.compose)

    // Compose：先用 BOM 锁定一整套兼容版本，再引入具体库
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.graphics)
    implementation(libs.compose.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.core)

    // 数据层：工作区列表的存储（DataStore Preferences，读写都是异步的）
    implementation(libs.datastore.preferences)

    // 只在 debug 构建里引入：让 Android Studio 的 Preview 能正常显示界面
    debugImplementation(libs.compose.tooling)
}
