rootProject.name = "Group"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public") }

        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev/") }

//        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
//        gradlePluginPortal()
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        mavenCentral()
        google()
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
//        // JetBrains Space (只提供 Kamel)
//        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev/")

        // 国内镜像
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        mavenCentral()
        google()
        
        // 添加 WebRTC KMP 库的仓库
        maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/") }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":composeApp")