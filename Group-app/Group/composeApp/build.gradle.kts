plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.wire)
}
val voyagerVersion = "1.1.0-beta02"
val camerax_version = "1.2.2"

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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

//    packagingOptions {
//        pickFirst("META-INF/AL2.0")
//        pickFirst("META-INF/LGPL2.1")
//        pickFirst("META-INF/licenses/ASM")
//    }
}

kotlin {
//    android() // 不用 androidTarget()，应用模块只注册 android() 即可
//    android {
//        compilations.all {
//            kotlinOptions.jvmTarget = "11"
//        }
//    }
    androidTarget {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
    }
//    jvm("desktop"){
//        compilations.all {
//            kotlinOptions.jvmTarget = "11"
//        }
//    }
//    js {
//        nodejs {
//            version = "20.11.1"
//        }
//        browser {
//            commonWebpackConfig {
//                outputFileName = "composeApp.js"
//                outputModuleName = "composeApp"
//            }
//        }
//        binaries.executable()
//    }

    sourceSets {
        val commonMain by getting {
            dependencies {

                implementation("io.github.aakira:napier:2.6.1")
                implementation("org.jetbrains.androidx.navigation:navigation-compose:2.9.0-beta01")

                implementation("com.squareup.okio:okio:3.7.0")

                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
                implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
                implementation(libs.kotlinx.coroutines.core)
                implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
                implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.9.0")


                implementation(libs.koin.compose.viewmodel.nav)
                implementation(libs.koin.compose.viewmodel)

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

//                implementation(project(":proto-wire")) // KMP library 依赖
                implementation(libs.wire.runtime)
                implementation(libs.kotlinx.serialization.json)
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.material3)
                implementation(libs.koin.core)
            }
            kotlin.srcDir("src/generated/kotlin") // Wire proto 输出目录

        }
//        val jvmMain by creating {
//            dependsOn(commonMain)
//            dependencies {
//                implementation(libs.wire.runtime)
//                implementation(compose.runtime)
//                implementation(compose.foundation)
//                // OkHttp for WebSocket connection
//                implementation("com.squareup.okhttp3:okhttp:4.12.0")
//                implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
//
//            }
//        }

        val androidMain by getting {
//            dependsOn(jvmMain) // Android 继承 jvmMain
            dependencies {

                //             图片文件预览
                implementation("io.coil-kt.coil3:coil-compose:3.3.0")
                implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")

                // 音视频播放
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
                implementation("com.shepeliev:webrtc-kmp:0.125.11")
            }
        }

        // 专注Android平台，移除desktop和js源集
        // val desktopMain by getting {
        //     dependsOn(jvmMain)
        //     dependencies {
        //         // desktop特定依赖
        //     }
        // }
        //
        // val jsMain by getting {
        //     dependencies {
        //         // js特定依赖
        //     }
        // }
    }
}
wire {
    kotlin {
//        val androidMain by getting{
//            android = false
//            javaInterop = false
//            out = "src/generated/kotlin"
//        }

        android = false
        javaInterop = false
//        out = "src/generated/kotlin"
    }
}

sqldelight {
    databases {
        create("AppDatabase") {
            packageName.set("com.github.im.group.db")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/db/migration"))
            verifyMigrations.set(true)
        }
    }
}
//   不在 KMP 中实现 desktop 和web 了
//compose.desktop {
//    application {
//        mainClass = "com.github.im.group.MainKt"
//        nativeDistributions {
//            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
//            packageName = "Group"
//            packageVersion = "1.0.0"
//        }
//    }
//}