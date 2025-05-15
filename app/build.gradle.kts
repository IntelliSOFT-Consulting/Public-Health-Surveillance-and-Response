plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
}

android {
  namespace = "com.icl.surveillance"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.icl.surveillance"
    minSdk = 26
    targetSdk = 35
    versionCode = 7
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    isCoreLibraryDesugaringEnabled = true
  }
  kotlinOptions { jvmTarget = "17" }
  buildFeatures { viewBinding = true }
  packagingOptions {
    resources.excludes.addAll(listOf("META-INF/ASL-2.0.txt", "META-INF/LGPL-3.0.txt"))
  }
}

dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.appcompat)
  implementation(libs.material)
  implementation(libs.androidx.constraintlayout)
  implementation(libs.androidx.lifecycle.livedata.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.ktx)
  implementation(libs.androidx.navigation.fragment.ktx)
  implementation(libs.androidx.navigation.ui.ktx)
  implementation(libs.androidx.activity)
    implementation(libs.firebase.crashlytics)
    testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  implementation("com.google.android.fhir:engine:1.2.0")
  coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
  implementation("com.google.android.fhir:data-capture:1.3.0")
  implementation("androidx.fragment:fragment-ktx:1.8.6")
  implementation("androidx.datastore:datastore-preferences:1.1.4")
  implementation("androidx.work:work-runtime-ktx:2.10.0")
  implementation ("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
  implementation("com.squareup.retrofit2:retrofit:2.9.0")
  implementation("com.squareup.retrofit2:converter-gson:2.9.0")
  implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
  implementation("com.squareup.retrofit2:converter-moshi:2.6.0")
  implementation("de.hdodenhof:circleimageview:3.1.0")
  implementation("com.github.f0ris.sweetalert:library:1.6.2")
  
}
