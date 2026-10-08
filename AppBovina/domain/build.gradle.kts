import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

// Dominio puro: aquí no entra ninguna dependencia de Android ni de Room (ver docs/adr/0001).
dependencies {
    api(libs.kotlinx.coroutines.core)
}
