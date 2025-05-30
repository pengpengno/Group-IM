
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.google.protobuf") version "0.9.4"
    kotlin("plugin.serialization") version libs.versions.kotlin.get()  // 注意：这个插件一定要加
    id("org.springframework.boot") version "3.1.2" // Spring Boot 3.1.x (包含 Spring Framework 6)
    kotlin("plugin.spring") version libs.versions.kotlin.get()  // 这里必须加
    id("io.spring.dependency-management") version "1.1.0"


    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
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
//    jvm {
//        withJava()
//        compilations.all {
//            kotlinOptions.jvmTarget = "17"
//        }
//    }
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


        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation("io.ktor:ktor-client-core:2.3.0")
            implementation("io.ktor:ktor-client-content-negotiation:2.3.0")
            implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.0")
            implementation("io.ktor:ktor-client-logging:2.3.0")
            implementation("io.ktor:ktor-client-cio:2.3.0") // for JVM/Desktop
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
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
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
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
    implementation("org.springframework.boot:spring-boot-starter")
    runtimeOnly("androidx.compose.material:material-icons-core:1.7.8")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    // Spring Boot Starter WebFlux (包含 WebClient, Reactor 等)
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("androidx.compose.foundation:foundation-layout-android:1.8.2")
    implementation("androidx.compose.material3:material3-android:1.3.2")
    debugImplementation(compose.uiTooling)
    implementation("io.vertx:vertx-core:4.5.1")
    implementation("io.vertx:vertx-tcp-eventbus-bridge:4.5.1") // 如使用事件总线
    // https://mvnrepository.com/artifact/com.google.protobuf/protobuf-java
    //noinspection UseTomlInstead
    implementation(libs.protobuf.java)
}

protobuf {
    protoc {
        // 4. 指定 protoc（Protobuf 编译器）的 Maven 坐标。插件会下载这个版本并调用它去生成代码
//        artifact = libs.protobuf.java.get().toString()
        artifact = "com.google.protobuf:protoc:3.21.12"  // 指定 protoc 编译器版本
    }

    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.github.im.group.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.github.im.group"
            packageVersion = "1.0.0"
        }
    }
}
