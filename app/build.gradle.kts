plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("com.google.gms.google-services")
    id ("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("kotlin-parcelize")
}

android {
    namespace = "com.marcosdeuna.unilink"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.marcosdeuna.unilink"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures{
        viewBinding = true
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
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(platform("com.google.firebase:firebase-bom:32.8.1"))
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-android-compiler:2.48")
    implementation("com.tbuonomo:dotsindicator:5.0")
    implementation("com.google.firebase:firebase-auth:19.2.0")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("com.google.firebase:firebase-firestore:24.1.2")
    implementation("com.google.firebase:firebase-storage-ktx:20.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.3.1")
    implementation("com.squareup.picasso:picasso:2.5.2")
    implementation("com.google.android.material:material:1.4.0")
    implementation("com.github.yuyakaido:CardStackView:2.3.4")
    implementation("com.github.bumptech.glide:glide:4.12.0")
    implementation("com.github.yalantis:ucrop:2.2.6")

}
kapt {
    correctErrorTypes = true
}