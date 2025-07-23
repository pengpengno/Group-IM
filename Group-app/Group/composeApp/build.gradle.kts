
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
val voyagerVersion = "1.1.0-beta02"

plugins {
    id("com.squareup.wire") version "4.8.1"

//    id("com.google.protobuf") version "0.9.4"
//    kotlin("plugin.serialization") version libs.versions.kotlin.get()  // 注意：这个插件一定要加
//    id("org.springframework.boot") version "3.1.2" // Spring Boot 3.1.x (包含 Spring Framework 6)
//    kotlin("plugin.spring") version libs.versions.kotlin.get()  // 这里必须加
//    id("io.spring.dependency-management") version "1.1.0"

    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
//    id("org.jetbrains.kotlin.android")

}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm("desktop")
    
    sourceSets {
        val commonMain by getting{
//            proto.srcDir("src/commonMain/proto")
        }
        val androidMain by getting
        val desktopMain by getting

        // 挂载 Protobuf 生成的 Java 源码
//        androidMain.kotlin.srcDir("build/generated/source/proto/main/java")
//        desktopMain.kotlin.srcDir("build/generated/source/proto/main/java")
        val camerax_version = "1.2.2"


        androidMain.dependencies {
            implementation ("androidx.camera:camera-core:${camerax_version}")
            implementation ("androidx.camera:camera-camera2:${camerax_version}")
            implementation ("androidx.camera:camera-lifecycle:${camerax_version}")
            implementation ("androidx.camera:camera-video:${camerax_version}")
            implementation ("androidx.camera:camera-extensions:${camerax_version}")
            implementation ("androidx.camera:camera-view:${camerax_version}")
            // Hilt integration
            implementation("cafe.adriel.voyager:voyager-hilt:$voyagerVersion")
            implementation("com.google.accompanist:accompanist-permissions:0.35.0-alpha")

            // LiveData integration
            implementation("cafe.adriel.voyager:voyager-livedata:$voyagerVersion")
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.android)
            implementation(libs.android.driver)
            implementation(libs.androidx.compose.material3)
            implementation(libs.koin.androidx.compose)
            implementation(libs.androidx.lifecycle.viewmodel.compose)

        }
        commonMain.dependencies {

            implementation("com.squareup.wire:wire-runtime:4.8.1")

            implementation("cafe.adriel.voyager:voyager-navigator:$voyagerVersion")
            implementation("cafe.adriel.voyager:voyager-screenmodel:$voyagerVersion")
            implementation("cafe.adriel.voyager:voyager-bottom-sheet-navigator:$voyagerVersion")
            implementation("cafe.adriel.voyager:voyager-tab-navigator:$voyagerVersion")
            implementation("cafe.adriel.voyager:voyager-transitions:$voyagerVersion")
            implementation("cafe.adriel.voyager:voyager-koin:$voyagerVersion")


            implementation("org.jetbrains.androidx.navigation:navigation-compose:2.9.0-beta01")

            implementation("com.squareup.okio:okio:3.7.0")

            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
            implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
            implementation(libs.kotlinx.coroutines.core)
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.9.0")


            implementation(libs.koin.compose.viewmodel.nav)
            implementation(libs.koin.compose.viewmodel)
//            implementation("io.insert-koin:koin-core")
            implementation(compose.material)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.runtime)
            implementation(libs.kotlinx.datetime)
            implementation(libs.koin.core)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.native.driver)
        }

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}

wire {
    kotlin {
        // ✅ 这表示会生成 Kotlin 数据类到 commonMain 下
        // 默认生成到 build/generated/source/wire
        // 你可以自定义 outputPath
        android = false         // 如果你希望针对 JVM，而不是 Android 目标
        javaInterop = false
    }
}

// 数据库插件
sqldelight {
//    ./gradlew generateCommonMainAppDatabaseInterface
    databases {
        create("AppDatabase") {
//            packageName.set("com.github.im.group.db")
            packageName.set("db")
        }
    }
}
android {
    namespace = "com.github.im.group"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.github.im.group"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    debugImplementation(compose.uiTooling)

}

compose.desktop {
    application {
        mainClass = "com.github.im.group.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Group"
            packageVersion = "1.0.0"
        }
    }
}
