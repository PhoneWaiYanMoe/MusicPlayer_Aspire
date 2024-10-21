plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.asphire"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.asphire"
        minSdk = 26
        targetSdk = 34
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
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
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

    //recyclerview animators
    implementation("jp.wasabeef:recyclerview-animators:4.0.2")

    //explorer
    implementation("com.google.android.exoplayer:exoplayer:2.19.1")

    //circular image
    implementation("de.hdodenhof:circleimageview:3.1.0")

    //audio visualizer
    implementation("io.github.gautamchibde:audiovisualizer:2.2.5")

    //for palettes for extracting colors
    implementation("androidx.palette:palette:1.0.0")

    //blurImageView
    implementation("com.github.jgabrielfreitas:BlurImageView:1.0.1")
}