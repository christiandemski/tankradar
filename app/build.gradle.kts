plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Signaturschlüssel deploy/tankradar.keystore, das Passwort steht daneben in deploy/tankradar.keystore.txt (beides nicht in Git)
val keystore = rootProject.file("deploy/tankradar.keystore")
val keystorePassword = rootProject.file("deploy/tankradar.keystore.txt").takeIf { it.exists() }?.readLines()
    ?.firstNotNullOfOrNull { Regex("^Passwort[^:]*: (.+)").find(it)?.groupValues?.get(1)?.trim() }

android {
    namespace = "de.codekorb.tankradar"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.codekorb.tankradar"
        minSdk = 30
        targetSdk = 36
        versionCode = 2 // vor jedem neuen Paket um eins erhöhen, sonst nimmt Android es nicht als Update an
        versionName = "2"
    }
    signingConfigs {
        if (keystore.exists() && keystorePassword != null) create("release") {
            storeFile = keystore; storePassword = keystorePassword
            keyAlias = "tankradar"; keyPassword = keystorePassword
        }
    }
    buildTypes {
        release { signingConfig = signingConfigs.findByName("release") }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.car.app:app:1.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
