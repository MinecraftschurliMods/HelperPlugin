plugins {
    `kotlin-dsl`
    `maven-publish`
    kotlin("plugin.serialization") version embeddedKotlinVersion
}

group = "com.github.minecraftschurlimods"
version = "2.0"
base.archivesName = "helperplugin"

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

repositories {
    maven { url = uri("https://maven.neoforged.net/releases") }
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.akuleshov7:ktoml-core:0.7.1")
    implementation("net.neoforged.gradle:userdev:[7.1.21,)")
}

testing {
    suites {
        withType<JvmTestSuite> {
            targets.all {
                testTask.configure {
                    testLogging {
                        events("failed")
                        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
                    }
                }
            }
        }
        named<JvmTestSuite>("test") {
            useJUnitJupiter("5.13.4")
        }
        // runs real gradle builds with NeoGradle (slow, needs network), so it is not part of check
        register<JvmTestSuite>("functionalTest") {
            useJUnitJupiter("5.13.4")
            dependencies {
                implementation(gradleTestKit())
            }
            targets.all {
                testTask.configure {
                    shouldRunAfter(tasks.test)
                }
            }
        }
    }
}

gradlePlugin {
    testSourceSets(sourceSets["functionalTest"])
    plugins {
        create("helper") {
            id = "com.github.minecraftschurlimods.helperplugin"
            displayName = "Helper Plugin"
            description = "A gradle helper plugin built on-top of the neoforged/NeoGradle plugin"
            implementationClass = "com.github.minecraftschurlimods.helperplugin.HelperPlugin"
        }
    }
}

publishing.publications.withType<MavenPublication> {
    artifactId = base.archivesName.get()
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
