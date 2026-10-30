plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val permanentSigning = providers.environmentVariable("PROFELAB_SIGN_RELEASE").orNull == "true"
fun signingEnv(name: String): String = providers.environmentVariable(name).orNull
    ?.takeIf { it.isNotBlank() } ?: error("Falta configuración de firma: $name")
val proposedVersionCode = providers.environmentVariable("PROFELAB_VERSION_CODE").orNull?.let {
    it.toIntOrNull()?.takeIf { value -> value in 2..2100000000 }
        ?: error("PROFELAB_VERSION_CODE debe ser un entero entre 2 y 2100000000")
} ?: 5

android {
    namespace = "pe.profelab.app"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        applicationId = "pe.profelab.app"
        targetSdk = 36
        versionCode = proposedVersionCode
        versionName = "1.0.2"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    androidResources { noCompress += "mp4" }
    signingConfigs {
        if (permanentSigning) create("permanent") {
            storeFile = file(signingEnv("PROFELAB_KEYSTORE_PATH"))
            check(requireNotNull(storeFile).isFile) { "No existe el keystore temporal" }
            storePassword = signingEnv("PROFELAB_KEYSTORE_PASSWORD")
            keyAlias = signingEnv("PROFELAB_KEY_ALIAS")
            keyPassword = signingEnv("PROFELAB_KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            if (permanentSigning) signingConfig = signingConfigs.getByName("permanent")
        }
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
kotlin { jvmToolchain(17) }

gradle.taskGraph.whenReady {
    if (allTasks.any { it.path in setOf(":app:assembleRelease", ":app:bundleRelease", ":app:packageRelease") }) {
        check(permanentSigning) { "Release requiere PROFELAB_SIGN_RELEASE=true y configuración de firma completa" }
    }
}

dependencies {
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

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
    androidTestImplementation(libs.test.core)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.junit)
    androidTestImplementation(libs.datastore)
}
