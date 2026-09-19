import java.io.File
import java.nio.charset.StandardCharsets
import java.util.HashMap

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
        // Keystore de signature auto-signée pour la distribution sideload.
        // Les secrets (alias, chemin, mot de passe) viennent de l'environnement
        // ou de .signing/keystore.env (gitignoré) — AUCUN mot de passe en dur.
        // Voir le helper signingSecret() en bas de fichier.
        create("release") {
            keyAlias = signingSecret("PAPRA_KEY_ALIAS")
            keyPassword = signingSecret("PAPRA_KEY_PASSWORD")
            storeFile = file(signingSecret("PAPRA_KEYSTORE"))
            storePassword = signingSecret("PAPRA_KEYSTORE_PASSWORD")
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

    // Tests unitaires JVM (exécutés par `./gradlew test`, sans appareil).
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}

// ─── Secrets de signature ────────────────────────────────────────────────────
// Résout une variable de signature en préférant l'environnement, puis un
// fichier .signing/keystore.env (gitignoré). Refuse le build si le secret
// manque — pas de mot de passe de secours ni de valeur codée en dur.
private fun signingSecret(name: String): String {
    val fromEnv = System.getenv(name)
    if (fromEnv != null && !fromEnv.isEmpty()) return fromEnv

    val fromFile = loadSigningEnvFile().get(name)
    if (fromFile != null && !fromFile.isEmpty()) return fromFile

    throw GradleException(
        "Signing secret '$name' is not set. Copy .signing/keystore.env.example to " +
            ".signing/keystore.env (or export the PAPRA_* variables) before building release.",
    )
}

// Lit .signing/keystore.env : lignes KEY=VALUE, ignore les lignes vides/#.
private fun loadSigningEnvFile(): Map<String, String> {
    val map = HashMap<String, String>()
    val envFile = File("${projectDir}/../.signing/keystore.env")
    if (envFile.isFile() && envFile.length() > 0) {
        val content = String(envFile.readBytes(), StandardCharsets.UTF_8)
        content.split("\n").forEach { raw ->
            val line = raw.trim()
            if (!line.isEmpty() && !line.startsWith("#")) {
                val idx = line.indexOf('=')
                if (idx > 0) {
                    map.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim())
                }
            }
        }
    }
    return map
}