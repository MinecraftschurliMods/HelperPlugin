plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven { url = uri("https://maven.neoforged.net/releases") }
}

dependencies {
    implementation("net.neoforged.gradle:userdev:7.0.80")
}

gradlePlugin {
    plugins {
        create("helper") {
            id = "com.github.minecraftschurlimods.helperplugin"
            implementationClass = "com.github.minecraftschurlimods.helperplugin.HelperPlugin"
        }
    }
}
