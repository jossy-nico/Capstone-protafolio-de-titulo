plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.frutosapp"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.frutosapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"http://10.0.2.2:5173/\""
            )
        }

        release {
            isMinifyEnabled = false

            val apiUrl = providers.gradleProperty("API_BASE_URL")
                .orElse("")

            val esCompilacionRelease = gradle.startParameter.taskNames.any {
                it.contains("Release", ignoreCase = true) ||
                it.contains("Bundle", ignoreCase = true)
            }

            if (esCompilacionRelease) {
                require(apiUrl.get().startsWith("https://")) {
                    "La version release requiere API_BASE_URL con HTTPS"
                }
            }

            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"${apiUrl.get()}\""
            )

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Retrofit & Coroutines for microservicio productos
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")

    //navegación
    implementation("androidx.navigation:navigation-compose:2.7.7")
    
    //Coil para cargar imagenes por url en compose
    implementation("io.coil-kt:coil-compose:2.7.0")

    // OkHttp (necesario para MultipartBody.Part)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

// Opcional: ver logs de requests (muy útil para debug)
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")


    implementation("androidx.compose.material:material-icons-extended")


    // Room: carrito persistente para invitados
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)


}