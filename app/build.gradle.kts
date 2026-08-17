import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

// أسرار محلية (secrets.properties) — خارج نظام Git.
val secretsProperties = Properties().apply {
    val secretsFile = rootProject.file("secrets.properties")
    if (secretsFile.exists()) {
        secretsFile.inputStream().use { load(it) }
    }
}

fun secretOrEmpty(name: String): String =
    secretsProperties.getProperty(name)?.trim().orEmpty()

// تُطبَّق إضافة google-services فقط عند وجود ملف الإعداد،
// حتى يُبنى المشروع بنجاح قبل ربط Firebase (انظر README).
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.nba.plus"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nba.plus"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        // مفتاح NewsData للاتصال المباشر قبل ربط دالة Supabase (وضع تطوير فقط).
        buildConfigField("String", "NEWSDATA_API_KEY", "\"${secretOrEmpty("NEWSDATA_API_KEY")}\"")
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"${secretOrEmpty("GOOGLE_WEB_CLIENT_ID").ifBlank { "872343695367-19gvum3637gp9801o2gnfi1d1gjvpve0.apps.googleusercontent.com" }}\""
        )
        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${secretOrEmpty("SUPABASE_URL").ifBlank { "https://tbnwlsucgfcpwajerjuw.supabase.co" }}\""
        )
        buildConfigField(
            "String",
            "SUPABASE_ANON_KEY",
            "\"${secretOrEmpty("SUPABASE_ANON_KEY").ifBlank { "sb_publishable_7Ag4mo1vX7jpLvOxeUV_wQ_w4UHz-9Q" }}\""
        )
        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"${secretOrEmpty("GEMINI_API_KEY").ifBlank { "AQ.Ab8RN6K3bpeiHDZb-JqtYpuJ3Q1sSln13jbMie9p4LYwaDs9MA" }}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // AndroidX أساسيات
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // التنقل + Hilt
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-android-compiler:2.52")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Paging 3
    implementation("androidx.paging:paging-runtime:3.3.5")
    implementation("androidx.paging:paging-compose:3.3.5")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // الشبكة: Retrofit + OkHttp + kotlinx.serialization
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // الصور
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Supabase (مصادقة + Postgrest للمزامنة)
    implementation("io.github.jan-tennert.supabase:gotrue-kt:2.6.1")
    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.6.1")
    implementation("io.ktor:ktor-client-android:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")

    // Firebase Cloud Messaging & Google Sign-In
    implementation("com.google.firebase:firebase-messaging:24.1.0")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // اختبارات الوحدة
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
