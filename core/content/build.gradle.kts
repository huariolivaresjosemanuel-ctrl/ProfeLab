plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "pe.profelab.core.content"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin { jvmToolchain(17) }

dependencies {
    implementation(project(":core:files"))
    implementation(libs.androidx.core)
    implementation(libs.coroutines.android)
    implementation(project(":core:model"))
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    // JSONObject is real in JVM tests, rather than Android's throwing unit-test stub.
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.junit)
}
