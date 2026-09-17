plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "app.paprashare"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.paprashare"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        // Keystore de signature (auto-signée) pour la distribution sideload.
        // Les chemins/mots de passe viennent des variables d'environnement avec
        // des valeurs locales de secours. La keystore vit hors git (.signing/).
        create("release") {
            keyAlias = System.getenv("PAPRA_KEY_ALIAS") ?: "papra"
            keyPassword = System.getenv("PAPRA_KEY_PASSWORD") ?: "papra-share-signing-2026"
            storeFile = file(
                System.getenv("PAPRA_KEYSTORE") ?: "${projectDir}/../.signing/papra-share-release.jks",
            )
            storePassword = System.getenv("PAPRA_KEYSTORE_PASSWORD") ?: "papra-share-signing-2026"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
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
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.ui.tooling)
}