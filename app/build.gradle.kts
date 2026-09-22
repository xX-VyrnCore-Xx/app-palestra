import com.android.build.gradle.api.ApkVariantOutput
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
}

// Bump versionCode by 1 on every release; versionName follows semver (MAJOR.MINOR.PATCH).
val appVersionCode = 3
val appVersionName = "1.2.0"

android {
    namespace = "com.vyrncore.palestra"
    compileSdk = 35

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    base.archivesName.set("VibeFitness-$appVersionName")

    val localProperties = Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) load(file.inputStream())
    }

    defaultConfig {
        applicationId = "com.vyrncore.palestra"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${localProperties.getProperty("SUPABASE_URL", "")}\""
        )
        buildConfigField(
            "String",
            "SUPABASE_ANON_KEY",
            "\"${localProperties.getProperty("SUPABASE_ANON_KEY", "")}\""
        )
        // URL of the PT web management app (Vercel). PTs no longer use this Android app: when
        // one signs in they get a screen pointing here instead. Blank = the button is hidden.
        buildConfigField(
            "String",
            "PT_WEB_APP_URL",
            "\"${localProperties.getProperty("PT_WEB_APP_URL", "")}\""
        )
    }

    // Release signing: reads from env vars (CI) or local.properties (local release builds).
    // Falls back to null (unsigned) when nothing is configured, so debug builds and CI runs
    // without release secrets still work — see docs/play_store_release.md for setup.
    fun releaseConfigValue(envName: String, propertyName: String): String? =
        System.getenv(envName)?.takeIf { it.isNotBlank() }
            ?: localProperties.getProperty(propertyName)?.takeIf { it.isNotBlank() }

    val releaseStorePath = releaseConfigValue("RELEASE_STORE_FILE", "RELEASE_STORE_FILE")
    val releaseStorePassword = releaseConfigValue("RELEASE_STORE_PASSWORD", "RELEASE_STORE_PASSWORD")
    val releaseKeyAlias = releaseConfigValue("RELEASE_KEY_ALIAS", "RELEASE_KEY_ALIAS")
    val releaseKeyPassword = releaseConfigValue("RELEASE_KEY_PASSWORD", "RELEASE_KEY_PASSWORD")
    val hasReleaseSigningConfig = listOf(releaseStorePath, releaseStorePassword, releaseKeyAlias, releaseKeyPassword).all { !it.isNullOrBlank() }

    signingConfigs {
        if (hasReleaseSigningConfig) {
            create("release") {
                storeFile = file(releaseStorePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // A real release key is used when configured (Play Store uploads); otherwise fall
            // back to the auto-generated debug key so CI can still produce an installable,
            // optimized release APK for distribution via GitHub Releases without needing that
            // secret set up first. Swap to the "release" signingConfig once RELEASE_STORE_* is set.
            signingConfig = if (hasReleaseSigningConfig) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
        )
    }

    packaging {
        resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
    }

    // Every build (debug or release) is just "<app name>-<version>.apk" - no "-debug"/"-release"
    // suffix. The version alone is enough to tell builds apart; the build type isn't user-facing.
    applicationVariants.all {
        outputs.all {
            (this as ApkVariantOutput).outputFileName =
                "VibeFitness-$appVersionName.apk"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    // collectAsStateWithLifecycle: stops collecting ViewModel state when the UI is not
    // visible (backgrounded), avoiding wasted recompositions while the app is in background.
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.10.0")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Hilt DI, processed via KSP. Hilt 2.52's KSP2 aggregating processor had known
    // multi-round bugs ("unexpected jvm signature V" / "Expected @AndroidEntryPoint to have
    // a value") that kapt couldn't reliably substitute for either (kapt's own stub generator
    // has an unresolved Kotlin-2.1.x metadata-reading crash). 2.58 fixed the Kotlin
    // 2.x/KSP2 metadata handling upstream and is the last release still on AGP 8.x
    // (2.59+ requires AGP 9).
    implementation("com.google.dagger:hilt-android:2.58")
    ksp("com.google.dagger:hilt-android-compiler:2.58")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Room (offline-first local database)
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    // Preferences (theme setting)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Splash screen
    implementation("androidx.core:core-splashscreen:1.0.1")

    // WorkManager (background sync). Workers are built via a manual WorkerFactory
    // (data/work/AppWorkerFactory.kt), not androidx.hilt-work's @HiltWorker codegen — see
    // ReminderWorker.kt for why (a kapt/K2 metadata-reading incompatibility on CoroutineWorker
    // subclasses with @AssistedInject constructors).
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Supabase (Auth, Postgrest, Realtime) + Ktor engine
    val supabaseVersion = "3.1.4"
    implementation("io.github.jan-tennert.supabase:postgrest-kt:$supabaseVersion")
    implementation("io.github.jan-tennert.supabase:auth-kt:$supabaseVersion")
    implementation("io.github.jan-tennert.supabase:realtime-kt:$supabaseVersion")
    implementation("io.github.jan-tennert.supabase:storage-kt:$supabaseVersion")
    implementation("io.github.jan-tennert.supabase:functions-kt:$supabaseVersion")
    implementation("io.ktor:ktor-client-android:3.1.2")

    // Inline image thumbnails in chat attachments. coil-gif adds animated-GIF decoding so
    // exercises with a custom .gif demo URL animate directly in the workout cards.
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-gif:2.7.0")

    // Firebase Cloud Messaging: server-triggered push (new chat message / plan assignment)
    // delivered even when the app is killed - complements Supabase Realtime, which only
    // updates data while the process is alive.
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
