import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val voyagerVersion = "1.1.0-beta02"
val webRtcKmpVersion = "0.125.11"
plugins {
    id("com.squareup.wire") version "4.8.1"


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
//   todo ios 下webrtc 的 处理

//    cocoapods {
//        version = "1.0.0"
//        summary = "Shared module"
//        homepage = "not published"
//        ios.deploymentTarget = "13.0"
//
//        pod("WebRTC-SDK") {
//            version = "125.6422.05"
//            moduleName = "WebRTC"
//        }
//
//        podfile = project.file("../iosApp/Podfile")
//
//        framework {
//            baseName = "shared"
//            isStatic = true
//        }
//
//        xcodeConfigurationToNativeBuildType["CUSTOM_DEBUG"] = NativeBuildType.DEBUG
//        xcodeConfigurationToNativeBuildType["CUSTOM_RELEASE"] = NativeBuildType.RELEASE
//    }
//   IOS TODO IOS 先不管
//    listOf(
//        iosX64(),
//        iosArm64(),
//        iosSimulatorArm64()
//    ).forEach { iosTarget ->
//        iosTarget.binaries.framework {
//            baseName = "ComposeApp"
//            isStatic = true
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
        val camerax_version = "1.2.2"


        androidMain.dependencies {
            //             图片文件预览
            implementation("io.coil-kt.coil3:coil-compose:3.3.0")
            implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")
            
            // 视频播放
            implementation("androidx.media3:media3-exoplayer:1.4.1")
            implementation("androidx.media3:media3-ui:1.4.1")

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

            // OkHttp for WebSocket connection
            implementation("com.squareup.okhttp3:okhttp:4.12.0")
            implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
            
            // WebRTC support - Only for Android
            implementation("com.shepeliev:webrtc-kmp:${webRtcKmpVersion}")
        }
        commonMain.dependencies {

//            implementation("cafe.adriel.voyager:voyager-navigator:${voyagerVersion}")
//            implementation("cafe.adriel.voyager:voyager-screenmodel:${voyagerVersion}")
//            implementation("cafe.adriel.voyager:voyager-bottom-sheet-navigator:${voyagerVersion}")
//            implementation("cafe.adriel.voyager:voyager-tab-navigator:${voyagerVersion}")
//            implementation("cafe.adriel.voyager:voyager-transitions:${voyagerVersion}")
//            implementation("cafe.adriel.voyager:voyager-koin:${voyagerVersion}")
            // 日志工程
            implementation("io.github.aakira:napier:2.6.1")

            implementation("com.squareup.wire:wire-runtime:4.8.1")


            implementation("org.jetbrains.androidx.navigation:navigation-compose:2.9.0-beta01")

            implementation("com.squareup.okio:okio:3.7.0")

            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
            implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
            implementation(libs.kotlinx.coroutines.core)
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.9.0")


            implementation(libs.koin.compose.viewmodel.nav)
            implementation(libs.koin.compose.viewmodel)

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
            
            // Kotlinx serialization
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
//        iosMain.dependencies {
//            implementation(libs.ktor.client.darwin)
//            implementation(libs.native.driver)
//        }

        desktopMain.dependencies {
            implementation("org.openjfx:javafx-base:20")
            implementation("org.openjfx:javafx-media:20")
            implementation("org.openjfx:javafx-controls:20")
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)

            // VLCJ for video playback on desktop
            implementation("uk.co.caprica:vlcj:4.8.2")
            // Note: No WebRTC dependency for desktop as webrtc-kmp doesn't support it
        }
    }
}

wire {
    kotlin {
        android = false         // 针对 JVM，而不是 Android 目标
        javaInterop = false
    }
}

// 数据库插件
sqldelight {
//    ./gradlew generateCommonMainAppDatabaseInterface
    databases {
        create("AppDatabase") {
            packageName.set("com.github.im.group.db")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/db/migration"))
            verifyMigrations.set(true)
//            packageName.set("db")
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
    // 添加 packagingOptions 以解决冲突
    packagingOptions {
        pickFirst("META-INF/AL2.0")
        pickFirst("META-INF/LGPL2.1")
        pickFirst("META-INF/licenses/ASM")
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
//
//// 自动生成 SQLDelight 文件的 Gradle Task
//tasks.register("generateSqlDelight") {
//    //TODO 等待 自动生成任务
//
//    group = "sqldelight"
//    description = "Generate .sq files from Kotlin data classes"
//    dependsOn("compileKotlinMetadata") // 先编译 commonMain
//
//    doLast {
//        // 1. Kotlin 数据类包路径
//        val entityPackage = "com.github.im.group.db.entities" // 你的实体包
//
//        // 2. 输出路径
//        val outputDir =
//            File(layout.buildDirectory.get().asFile, "sqldelight/com/github/im/group/db")
//        outputDir.mkdirs()
//
//        // 3. 反射扫描实体类
//        val classLoader = Thread.currentThread().contextClassLoader
//        val classes = Class.forName(entityPackage + "", true, classLoader).kotlin // 可扩展扫描多个类
//
//        // ✅ 手动维护实体列表
//        val entities: List<KClass<*>> = listOf(
////            com.github.im.group.db.entities.ChatMessage::class,
////            com.github.im.group.db.entities.Conversation::class,
////            com.github.im.group.db.entities.User::class
//        )
//
//        entities.forEach { kClass ->
//            val tableName = kClass.simpleName!!.replaceFirstChar { it.lowercase() }
//            fun generateSql(kClass: KClass<*>): String {
//                val tableName = kClass.simpleName!!.replaceFirstChar { it.lowercase() }
//
//                val columns = kClass.memberProperties.joinToString(",\n") { prop ->
//                    val sqlType = when (prop.returnType.toString()) {
//                        "kotlin.Long", "kotlin.Long?" -> "INTEGER"
//                        "kotlin.Int", "kotlin.Int?" -> "INTEGER"
//                        "kotlin.String", "kotlin.String?" -> "TEXT"
//                        "kotlin.Boolean", "kotlin.Boolean?" -> "INTEGER"
//                        else -> "TEXT"
//                    }
//                    val primaryKey = if (prop.name == "msgId") " PRIMARY KEY AUTOINCREMENT" else ""
//                    "${prop.name} $sqlType$primaryKey"
//                }
//
//                val columnNames = kClass.memberProperties.joinToString(", ") { it.name }
//                val columnParams = kClass.memberProperties.joinToString(", ") { "?" }
//
//                return """
//                CREATE TABLE $tableName (
//                    $columns
//                );
//
//                selectAll:
//                SELECT * FROM $tableName;
//
//                insert:
//                INSERT INTO $tableName($columnNames) VALUES ($columnParams);
//
//                deleteById:
//                DELETE FROM $tableName WHERE msgId = ?;
//            """.trimIndent()
//            }
//
//            // 生成 SQL 文件
//            val sqlFile = File(outputDir, "${classes.simpleName}.sq")
//            sqlFile.writeText(generateSql(classes))
//
//            Napier.d("Generated SQLDelight file: ${sqlFile.absolutePath}")
//        }
//    }
//}