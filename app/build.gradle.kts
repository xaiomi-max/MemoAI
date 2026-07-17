import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}
val deepseekApiKey = (localProperties.getProperty("DEEPSEEK_API_KEY") ?: "")
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
val iflytekAppId = (localProperties.getProperty("IFLYTEK_APP_ID") ?: "3d0f8f93")
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
val iflytekApiKey = (localProperties.getProperty("IFLYTEK_API_KEY") ?: "")
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
val iflytekApiSecret = (localProperties.getProperty("IFLYTEK_API_SECRET") ?: "")
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

android {
    namespace = "com.memoai.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.memoai.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        // 当前为 vivo 应用商店版本；后续可按 flavor 覆盖，见下方 productFlavors 模板
        buildConfigField("String", "DISTRIBUTION_CHANNEL", "\"vivo\"")
        buildConfigField("String", "DEEPSEEK_API_KEY", "\"$deepseekApiKey\"")
        buildConfigField("String", "IFLYTEK_APP_ID", "\"$iflytekAppId\"")
        buildConfigField("String", "IFLYTEK_API_KEY", "\"$iflytekApiKey\"")
        buildConfigField("String", "IFLYTEK_API_SECRET", "\"$iflytekApiSecret\"")
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs", "libs")
        }
    }

    // 后续按应用商店分包示例（取消注释并为各渠道单独 assembleRelease）：
    // flavorDimensions += "store"
    // productFlavors {
    //     create("vivo") {
    //         dimension = "store"
    //         buildConfigField("String", "DISTRIBUTION_CHANNEL", "\"vivo\"")
    //     }
    //     create("xiaomi") {
    //         dimension = "store"
    //         buildConfigField("String", "DISTRIBUTION_CHANNEL", "\"xiaomi\"")
    //         // 同时在 OemAutostartRouter.routes 中启用 XiaomiAutostartRoute
    //     }
    //     create("huawei") {
    //         dimension = "store"
    //         buildConfigField("String", "DISTRIBUTION_CHANNEL", "\"huawei\"")
    //     }
    //     create("oppo") {
    //         dimension = "store"
    //         buildConfigField("String", "DISTRIBUTION_CHANNEL", "\"oppo\"")
    //     }
    // }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(files("libs/SparkChain.aar"))
    implementation(files("libs/Codec.aar"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
