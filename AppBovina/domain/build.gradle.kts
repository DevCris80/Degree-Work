import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    // Los repositorios falsos viven en testFixtures para que los usen los tests de :domain y los de :app.
    `java-test-fixtures`
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

// Solo para pruebas: el código de producción de :domain sigue sin más dependencia que coroutines-core.
dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
