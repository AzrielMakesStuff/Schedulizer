import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget -> iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
    } }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview) // Defaults
            implementation(libs.androidx.activity.compose)

            implementation(libs.androidx.security.crypto.ktx) // Storage
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime) // Defaults
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.kotlinx.datetime) // Data

            implementation(libs.multiplatform.settings) // Storage

            implementation("com.kizitonwose.calendar:compose-multiplatform:2.10.1")
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)


            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")

            implementation(libs.navigation.compose)

            implementation(libs.screen.size)

            implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.ateam.schedulizer"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.ateam.schedulizer"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
    ksp(libs.androidx.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}
