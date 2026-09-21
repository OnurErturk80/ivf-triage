plugins {
    id("com.android.application")
}

// The web app at the repository root is the single source of truth. It is copied
// into the APK's assets at build time so the page never has to be kept in sync by hand.
val webAssets = layout.buildDirectory.dir("generated/webAssets")

val copyWebAssets by tasks.registering(Copy::class) {
    description = "Copies the root web app into the APK assets."
    from(rootProject.layout.projectDirectory.dir("..")) {
        include("index.html", "manifest.json", "icon-*.png")
    }
    into(webAssets)
    // sw.js is deliberately NOT bundled: inside the APK the assets are already local,
    // and a service-worker cache would be able to serve a stale page after an app update.
}

android {
    namespace = "io.github.onurerturk80.ivftriage"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.onurerturk80.ivftriage"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    sourceSets.getByName("main") {
        assets.srcDir(webAssets)
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.named("preBuild") { dependsOn(copyWebAssets) }

dependencies {
    implementation("androidx.activity:activity:1.9.3")
    implementation("androidx.core:core:1.13.1")
    implementation("androidx.webkit:webkit:1.12.1")
}
