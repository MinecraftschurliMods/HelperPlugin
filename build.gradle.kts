plugins {
    `kotlin-dsl`
    `maven-publish`
    kotlin("plugin.serialization") version "1.9.22"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven { url = uri("https://maven.neoforged.net/releases") }
}

group = "com.github.minecraftschurlimods"
version = "1.13"
base.archivesName = "HelperPlugin"

dependencies {
    implementation("com.akuleshov7:ktoml-core:0.5.1")
    implementation("net.neoforged.gradle:userdev:7.0.106")
}

gradlePlugin {
    plugins {
        create("helper") {
            id = "com.github.minecraftschurlimods.helperplugin"
            displayName = "Helper Plugin"
            description = "A gradle helper plugin built on-top of the neoforged/NeoGradle plugin"
            implementationClass = "com.github.minecraftschurlimods.helperplugin.HelperPlugin"
        }
    }
}

publishing.repositories.maven {
    val mavenUrl = project.providers.environmentVariable("MAVEN_URL").map { uri(it) }
    val mavenUser = project.providers.environmentVariable("MAVEN_USER")
    val mavenPassword = project.providers.environmentVariable("MAVEN_PASSWORD")
    if (mavenUrl.isPresent && mavenUser.isPresent && mavenPassword.isPresent) {
        url = mavenUrl.get()
        credentials {
            username = mavenUser.get()
            password = mavenPassword.get()
        }
    } else {
        println("Using repo folder")
        url = uri(layout.buildDirectory.dir("repo"))
    }
}
