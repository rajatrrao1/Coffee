plugins {
    id("com.android.application")
}

// Each build on GitHub gets a higher number, so the phone accepts it as an update.
val run = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "app.kohi"
    compileSdk = 34

    defaultConfig {
        applicationId = "app.kohi"
        minSdk = 28
        targetSdk = 34
        versionCode = run
        versionName = "1.0.$run"
    }

    // One fixed key, so a new build installs over the old one and keeps your beans.
    signingConfigs {
        create("coffee") {
            storeFile = file("../signing/kohi.keystore")
            storeType = "pkcs12"
            storePassword = "kohicoffee"
            keyAlias = "kohi"
            keyPassword = "kohicoffee"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("coffee")
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("coffee")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}
