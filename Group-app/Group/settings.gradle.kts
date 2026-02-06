rootProject.name = "Group"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        // 国内镜像（作为补充，不作为主源）
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")

        google()

        // Kotlin / Compose / Gradle plugins
        mavenCentral()
        gradlePluginPortal()

        // JetBrains Compose
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev/")


    }
}

dependencyResolutionManagement {

    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

    repositories {
//        // ---- Node.js distribution (给 kotlin js 用) ----
//        ivy("https://nodejs.org/dist/") {
//            patternLayout {
//                artifact("v[revision]/[artifact]-v[revision]-[classifier].[ext]")
//            }
//            metadataSources {
//                artifact()
//            }
//        }
//
//        // ---------- Yarn distribution ----------
//        ivy("https://github.com/yarnpkg/yarn/releases/download/") {
//            name = "yarn-dist"
//            patternLayout {
//                artifact("v[revision]/yarn-v[revision].tar.gz")
//            }
//            metadataSources {
//                artifact()
//            }
//        }

        // ⭐⭐⭐ aapt2 / AGP 必须
        google()

        // 主仓库
        mavenCentral()

        // JetBrains Compose
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev/")

        // 国内镜像（兜底）
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")

        // snapshots（你用 WebRTC / 实验性库才留）
//        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":composeApp")
