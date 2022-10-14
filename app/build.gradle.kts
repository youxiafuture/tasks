import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension

plugins {
    id("com.android.application")
    id("checkstyle")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    kotlin("android")
    kotlin("kapt")
    id("com.cookpad.android.plugin.license-tools") version "1.2.8"
    id("com.github.ben-manes.versions") version "0.39.0"
    id("com.vanniktech.android.junit.jacoco") version "0.16.0"
    id("dagger.hilt.android.plugin")
}

repositories {
    mavenCentral()
    google()
    maven {
        url = uri("https://jitpack.io")
        content {
            includeGroup("com.gitlab.abaker")
            includeModule("com.gitlab.bitfireAT", "cert4android")
            includeModule("com.github.tasks.opentasks", "opentasks-provider")
            includeModule("com.github.QuadFlask", "colorpicker")
            includeModule("com.github.twofortyfouram", "android-plugin-api-for-locale")
        }
    }
}

android {
    val commonTest = "src/commonTest/java"
    sourceSets["test"].java.srcDir(commonTest)
    sourceSets["androidTest"].java.srcDirs("src/androidTest/java", commonTest)

    bundle {
        language {
            enableSplit = false
        }
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        compose = true
    }

    lint {
        //disable("InvalidPeriodicWorkRequestInterval")
        lintConfig = file("lint.xml")
        //textOutput("stdout")
        textReport = true
    }

    compileSdk = Versions.compileSdk

    defaultConfig {
        testApplicationId = "org.tasks.test"
        applicationId = "org.tasks"
        versionCode = 120603
        versionName = "12.6.1"
        targetSdk = Versions.targetSdk
        minSdk = Versions.minSdk
        testInstrumentationRunner = "org.tasks.TestRunner"

        kapt {
            arguments {
                arg("room.schemaLocation", "$projectDir/schemas")
                arg("room.incremental", "true")
            }
        }
    }

    signingConfigs {
        create("release") {
            val tasksKeyAlias: String? by project
            val tasksStoreFile: String? by project
            val tasksStorePassword: String? by project
            val tasksKeyPassword: String? by project

            keyAlias = tasksKeyAlias
            storeFile = file(tasksStoreFile ?: "none")
            storePassword = tasksStorePassword
            keyPassword = tasksKeyPassword
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    composeOptions {
        kotlinCompilerExtensionVersion = Versions.compose
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    @Suppress("LocalVariableName")
    buildTypes {
        getByName("debug") {
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = false
            }
            val tasks_mapbox_key_debug: String? by project
            val tasks_google_key_debug: String? by project
            val tasks_caldav_url: String? by project
            resValue("string", "mapbox_key", tasks_mapbox_key_debug ?: "")
            resValue("string", "google_key", tasks_google_key_debug ?: "")
            resValue("string", "tasks_caldav_url", tasks_caldav_url ?: "https://caldav.tasks.org")
            resValue("string", "tasks_nominatim_url", tasks_caldav_url ?: "https://nominatim.tasks.org")
            resValue("string", "tasks_places_url", tasks_caldav_url ?: "https://places.tasks.org")
            isTestCoverageEnabled = project.hasProperty("coverage")
        }
        getByName("release") {
            val tasks_mapbox_key: String? by project
            val tasks_google_key: String? by project
            resValue("string", "mapbox_key", tasks_mapbox_key ?: "")
            resValue("string", "google_key", tasks_google_key ?: "")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    flavorDimensions("store")

    productFlavors {
        create("generic") {
            dimension = "store"
        }
        create("googleplay") {
            dimension = "store"
        }
    }

    packagingOptions {
        exclude("META-INF/*.kotlin_module")
    }
}

configure<CheckstyleExtension> {
    configFile = project.file("google_checks.xml")
    toolVersion = "8.16"
}

configurations.all {
    exclude(group = "org.apache.httpcomponents")
    exclude(group = "org.checkerframework")
    exclude(group = "com.google.code.findbugs")
    exclude(group = "com.google.errorprone")
    exclude(group = "com.google.j2objc")
    exclude(group = "com.google.http-client", module = "google-http-client-apache-v2")
    exclude(group = "com.google.http-client", module = "google-http-client-jackson2")
}

val genericImplementation by configurations
val googleplayImplementation by configurations

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.5")
    implementation("com.gitlab.abaker:dav4jvm:deb2c9aef8")
    implementation("com.gitlab.abaker:ical4android:0e928b567c")
    implementation("com.gitlab.bitfireAT:cert4android:26a91a729f")
    implementation("com.github.tasks.opentasks:opentasks-provider:a1faa1b") {
        exclude("com.github.tasks.opentasks", "opentasks-contract")
    }

    implementation("com.google.dagger:hilt-android:${Versions.hilt}")
    kapt("com.google.dagger:hilt-compiler:${Versions.hilt}")
    kapt("androidx.hilt:hilt-compiler:${Versions.hilt_androidx}")
    implementation("androidx.hilt:hilt-work:${Versions.hilt_androidx}")

    implementation("androidx.fragment:fragment-ktx:1.4.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:${Versions.lifecycle}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.lifecycle}")
    implementation("androidx.room:room-ktx:${Versions.room}")
    kapt("androidx.room:room-compiler:${Versions.room}")
    implementation("androidx.appcompat:appcompat:1.4.0")
    implementation("androidx.paging:paging-runtime:2.1.2")
    implementation("io.noties.markwon:core:${Versions.markwon}")
    implementation("io.noties.markwon:editor:${Versions.markwon}")
    implementation("io.noties.markwon:ext-tasklist:${Versions.markwon}")
    implementation("io.noties.markwon:ext-strikethrough:${Versions.markwon}")
    implementation("io.noties.markwon:ext-tables:${Versions.markwon}")
    implementation("io.noties.markwon:linkify:${Versions.markwon}")

    debugImplementation("com.facebook.flipper:flipper:${Versions.flipper}")
    debugImplementation("com.facebook.flipper:flipper-network-plugin:${Versions.flipper}")
    debugImplementation("com.facebook.soloader:soloader:0.10.3")
    debugImplementation("com.squareup.leakcanary:leakcanary-android:${Versions.leakcanary}")
    debugImplementation("androidx.compose.ui:ui-tooling:${Versions.compose}")
    debugImplementation("org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}")
    implementation("com.squareup.okhttp3:okhttp:${Versions.okhttp}")
    implementation("com.google.code.gson:gson:2.8.8")
    implementation("com.google.android.material:material:1.5.0-rc01")
    implementation("androidx.constraintlayout:constraintlayout:2.1.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.preference:preference:1.1.1")
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("com.google.android.apps.dashclock:dashclock-api:2.0.0")
    implementation("com.github.twofortyfouram:android-plugin-api-for-locale:1.0.2") {
        isTransitive = false
    }
    implementation("com.rubiconproject.oss:jchronic:0.2.6") {
        isTransitive = false
    }
    implementation("me.leolin:ShortcutBadger:1.1.22@aar")
    implementation("com.google.apis:google-api-services-tasks:v1-rev20210709-1.32.1")
    implementation("com.google.apis:google-api-services-drive:v3-rev20210725-1.32.1")
    implementation("com.google.auth:google-auth-library-oauth2-http:0.26.0")
    implementation("androidx.work:work-runtime:${Versions.work}")
    implementation("androidx.work:work-runtime-ktx:${Versions.work}")
    implementation("com.etebase:client:2.3.2")
    implementation("com.github.QuadFlask:colorpicker:0.0.15")
    implementation("net.openid:appauth:0.8.1")
    implementation("org.osmdroid:osmdroid-android:6.1.11@aar")

    implementation("androidx.compose.ui:ui:${Versions.compose}")
    implementation("androidx.compose.foundation:foundation:${Versions.compose}")
    implementation("androidx.compose.material:material:${Versions.compose}")
    implementation("androidx.compose.runtime:runtime-livedata:${Versions.compose}")
    implementation("com.google.android.material:compose-theme-adapter:${Versions.compose_theme_adapter}")
    implementation("androidx.activity:activity-compose:1.4.0")
    implementation("androidx.compose.material:material-icons-extended:${Versions.compose}")
    releaseCompileOnly("androidx.compose.ui:ui-tooling:${Versions.compose}")

    googleplayImplementation("com.google.firebase:firebase-crashlytics:${Versions.crashlytics}")
    googleplayImplementation("com.google.firebase:firebase-analytics:${Versions.analytics}") {
        exclude("com.google.android.gms", "play-services-ads-identifier")
    }
    googleplayImplementation("com.google.firebase:firebase-config-ktx:${Versions.remote_config}")
    googleplayImplementation("com.google.android.gms:play-services-location:19.0.1")
    googleplayImplementation("com.google.android.gms:play-services-maps:18.0.2")
    googleplayImplementation("com.android.billingclient:billing-ktx:3.0.3")
    googleplayImplementation("com.google.android.play:core:1.10.3")
    googleplayImplementation("com.google.android.play:core-ktx:1.8.1")

    androidTestImplementation("com.google.dagger:hilt-android-testing:${Versions.hilt}")
    kaptAndroidTest("com.google.dagger:hilt-compiler:${Versions.hilt}")
    kaptAndroidTest("androidx.hilt:hilt-compiler:${Versions.hilt_androidx}")
    androidTestImplementation("org.mockito:mockito-android:${Versions.mockito}")
    androidTestImplementation("com.natpryce:make-it-easy:${Versions.make_it_easy}")
    androidTestImplementation("androidx.test:runner:${Versions.androidx_test}")
    androidTestImplementation("androidx.test:rules:${Versions.androidx_test}")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:${Versions.okhttp}")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.5.2")
    testImplementation("com.natpryce:make-it-easy:${Versions.make_it_easy}")
    testImplementation("androidx.test:core:${Versions.androidx_test}")
    testImplementation("org.mockito:mockito-core:${Versions.mockito}")
    testImplementation("org.ogce:xpp3:1.1.6")
}
