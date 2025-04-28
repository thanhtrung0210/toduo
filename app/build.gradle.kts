plugins {
    alias(libs.plugins.android.application)

//    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    signingConfigs {
        create("my_config") {
            storeFile = file("E:\\Shy\\toduo\\keystore\\toduo_keystore.jks")
            storePassword = "1)Trung."
            keyAlias = "pipo"
            keyPassword = "1)Trung."
        }
    }
    namespace = "com.pipoxniko.toduo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pipoxniko.toduo"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("my_config")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(platform("com.google.firebase:firebase-bom:33.12.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
}