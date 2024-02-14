plugins {
    `kotlin-dsl`
    `maven-publish`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven { url = uri("https://maven.neoforged.net/releases") }
}

group = "com.github.minecraftschurlimods"
version = "1.0"
base.archivesName = "HelperPlugin"

dependencies {
    implementation("net.neoforged.gradle:userdev") {
        version {
            strictly("[7.0.80,7.1)")
            prefer("7.0.80")
        }
    }
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
