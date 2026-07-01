plugins {
    id("com.android.application")
}

android {
    namespace = "com.deepblue.etfnav"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.deepblue.etfnav"
        minSdk = 23
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.2"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
