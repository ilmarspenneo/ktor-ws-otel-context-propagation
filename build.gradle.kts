
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
}

group = "com.example"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.cio.EngineMain"

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.cio)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.config.yaml)

    implementation(libs.opentelemetry.ktor)
    implementation(libs.opentelemetry.sdk.extension.autoconfigure)
    implementation(libs.opentelemetry.instrumentation.annotations)
    implementation(libs.opentelemetry.instrumentation.logback.mdc)
    implementation(libs.opentelemetry.exporter.logging)
    implementation(libs.opentelemetry.exporter.otlp)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
}
