import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "app.unpeep.privacy"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.unpeep.privacy"
        minSdk = 26
        targetSdk = 36

        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // About screen links. They open in the browser.
        buildConfigField("String", "REPO_URL", "\"https://github.com/nisabmohd/unpeep\"")
        buildConfigField("String", "CONTACT_EMAIL", "\"hello.midapps@gmail.com\"")
        buildConfigField("String", "PLAY_STORE_URL", "\"https://play.google.com/store/apps/details?id=app.unpeep.privacy\"")
    }

    flavorDimensions += "distribution"
    productFlavors {
        // Google Play: In-App Review (src/play).
        create("play") {
            dimension = "distribution"
            isDefault = true
            buildConfigField("boolean", "PLAY_STORE", "true")
            buildConfigField("boolean", "PLAY_LISTED", "false") // true once live on Play: "Rate on Play Store" in About
            buildConfigField("boolean", "GITHUB_UPDATES", "false") // Play updates it
        }
        // F-Droid: no Google code at all.
        create("foss") {
            dimension = "distribution"
            versionNameSuffix = "-foss"
            buildConfigField("boolean", "PLAY_STORE", "false")
            buildConfigField("boolean", "PLAY_LISTED", "false")
            buildConfigField("boolean", "GITHUB_UPDATES", "false") // F-Droid updates it
        }
        // The APK on GitHub Releases: foss plus the update check (asks GitHub for the latest release, nothing else).
        create("github") {
            dimension = "distribution"
            versionNameSuffix = "-github"
            buildConfigField("boolean", "PLAY_STORE", "false")
            buildConfigField("boolean", "PLAY_LISTED", "false")
            buildConfigField("boolean", "GITHUB_UPDATES", "true")
        }
    }

    sourceSets {
        // src/play: Play's In-App Review. foss and github get the stand-in, so foss never contains Google code.
        // AGP 9 compiles Kotlin itself and reads Kotlin files from the `kotlin` source set (as Burrow and Weft).
        getByName("github").kotlin.srcDirs("src/noPlay/java")
        getByName("foss").kotlin.srcDirs("src/noPlay/java")
    }

    signingConfigs {
        // CI sets the env vars; locally the gitignored keystore/keystore.properties. Otherwise debug key.
        val keystore = System.getenv("UNPEEP_KEYSTORE_PATH")
        val local = rootProject.file("keystore/keystore.properties")
        if (keystore != null) {
            create("release") {
                storeFile = file(keystore)
                storePassword = System.getenv("UNPEEP_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("UNPEEP_KEY_ALIAS")
                keyPassword = System.getenv("UNPEEP_KEY_PASSWORD")
            }
        } else if (local.exists()) {
            val props = Properties().apply { local.inputStream().use { load(it) } }
            create("release") {
                storeFile = rootProject.file("keystore/" + props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // ar-XB (right-to-left English) and en-XA for checks.
            isPseudoLocalesEnabled = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    bundle {
        // The language can be changed inside the app, so every Play install needs every language.
        language { enableSplit = false }
    }

    androidResources {
        // Only the languages the app ships (keep in step with values-* folders).
        localeFilters += listOf("en", "de", "es", "fr", "hi", "ja", "pt-rBR")
        generateLocaleConfig = true
    }

    packaging {
        resources {
            excludes += listOf(
                "META-INF/**/LICENSE.txt",
                "META-INF/*.version",
                "META-INF/{AL2.0,LGPL2.1}",
                "kotlin/**.kotlin_builtins",
                "kotlin-tooling-metadata.json",
                "DebugProbesKt.bin",
            )
        }
    }

    dependenciesInfo {
        includeInApk = false // F-Droid rejects the signed dependency block
        includeInBundle = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

baselineProfile {
    automaticGenerationDuringBuild = false // ./gradlew :app:generateBaselineProfile, then commit
    mergeIntoMain = true
    dexLayoutOptimization = true
}

dependencies {
    "playImplementation"(libs.play.review)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))

    testImplementation(libs.junit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core) // 3.7: runs on Android 17 (see libs.versions.toml)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
