plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "org.quranscan.reader"
    compileSdk = 35
    defaultConfig { applicationId = "org.quranscan.reader"; minSdk = 23; targetSdk = 35; versionCode = 9; versionName = "0.9.0-dev" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}

// Compiled Tesseract Android/JNI runtime resolved at BUILD time. At runtime, fully offline.
dependencies {
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    testImplementation("junit:junit:4.13.2")
    implementation("cz.adaptech.tesseract4android:tesseract4android:4.9.0")
    // CameraX 1.4.2: stable, tested API baseline; runtime has no network permission.
    implementation("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")
    implementation("androidx.activity:activity:1.9.3")
}
