plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "pe.profelab.app"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        applicationId = "pe.profelab.app"
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-h1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    buildTypes { release { isMinifyEnabled = false } }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
kotlin { jvmToolchain(17) }

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.coroutines.android)
    implementation(project(":core:model"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.viewmodel)
    implementation(project(":core:ui"))
    implementation(project(":core:database"))
    implementation(project(":core:content"))
    implementation(project(":core:files"))
    implementation(project(":core:security"))
    implementation(project(":feature:home"))
    implementation(project(":feature:taller"))
    implementation(project(":feature:sessions"))
    implementation(project(":feature:notebook"))
    implementation(project(":feature:library"))
    implementation(project(":feature:pdf"))
    implementation(project(":feature:capture"))
    implementation(project(":feature:ocr"))
    implementation(project(":feature:profile"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.work)
    implementation(libs.lifecycle.process)
    debugImplementation(libs.compose.tooling)
    debugImplementation(libs.compose.test.manifest)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.test)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.junit)
}
