import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
}

val awsProperties = Properties().apply {
    val file = rootProject.file("aws_credentials.properties")
    if (file.exists()) {
        load(file.inputStream())
    }
}

android {
    namespace = "com.astutenotes"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.astutenotes"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        buildConfigField(
            "String", "AWS_ACCESS_KEY_ID",
            "\"${awsProperties.getProperty("aws.accessKeyId", "")}\""
        )
        buildConfigField(
            "String", "AWS_SECRET_ACCESS_KEY",
            "\"${awsProperties.getProperty("aws.secretAccessKey", "")}\""
        )
        buildConfigField(
            "String", "AWS_REGION",
            "\"${awsProperties.getProperty("aws.region", "us-east-1")}\""
        )
        buildConfigField(
            "String", "S3_BUCKET_NAME",
            "\"${awsProperties.getProperty("aws.s3.bucket", "")}\""
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/io.netty.versions.properties"
        }
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.navigation.compose)

    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    implementation(libs.aws.s3)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
}
