rootProject.name = "Group"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    // pluginManagement is evaluated in Gradle's early settings phase, so this
    // value must be declared inside the block rather than at top level.
    val useAliyunMirror = providers.gradleProperty("useAliyunMirror").orNull?.toBoolean()
        ?: System.getenv("CI") != "true"

    repositories {
//        maven("https://maven.aliyun.com/repository/google")
//        maven("https://maven.aliyun.com/repository/public")

        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev/")

        if (useAliyunMirror) {
            maven("https://maven.aliyun.com/repository/google")
            maven("https://maven.aliyun.com/repository/public")

        }
    }
}

dependencyResolutionManagement {
    // CI uses only official repositories. Local development may use Aliyun as a
    // fallback; override it with -PuseAliyunMirror=true or -PuseAliyunMirror=false.
    val useAliyunMirror = providers.gradleProperty("useAliyunMirror").orNull?.toBoolean()
        ?: System.getenv("CI") != "true"

    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

    repositories {
        // Node/Yarn Ivy repositories can be enabled here if Kotlin/JS needs them.
        // ivy("https://nodejs.org/dist/") { ... }
        // ivy("https://github.com/yarnpkg/yarn/releases/download/") { ... }

        // aapt2 / Android Gradle Plugin dependencies.
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev/")

        if (useAliyunMirror) {
//             Local fallback mirrors are deliberately excluded from CI.
            maven("https://maven.aliyun.com/repository/google")
            maven("https://maven.aliyun.com/repository/public")
        }

        // Add a snapshots repository only when a dependency explicitly requires it.
        // maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":composeApp")
