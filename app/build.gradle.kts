plugins { id("com.android.application") }

android {
  namespace = "hu.csoti35.akvarisztika"
  compileSdk = 36

  defaultConfig {
    applicationId = "hu.csoti35.akvarisztika"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"
  }

  signingConfigs {
    create("shared") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }
  buildTypes {
    debug { signingConfig = signingConfigs.getByName("shared") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}

dependencies {
  implementation("androidx.core:core:1.13.1")
}
