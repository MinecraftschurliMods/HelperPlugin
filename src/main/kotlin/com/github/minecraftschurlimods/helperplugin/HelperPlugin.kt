package com.github.minecraftschurlimods.helperplugin

import com.github.minecraftschurlimods.helperplugin.moddependencies.ModDependencyContainer
import com.github.minecraftschurlimods.helperplugin.moddependencies.wireModDependencies
import com.github.minecraftschurlimods.helperplugin.modstoml.GenerateModsTomlTask
import com.github.minecraftschurlimods.helperplugin.modstoml.WriteGitHubActionsOutputTask
import com.github.minecraftschurlimods.helperplugin.modstoml.toModsToml
import net.neoforged.gradle.dsl.common.extensions.subsystems.Conventions
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.external.javadoc.CoreJavadocOptions
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.attributes
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.repositories
import org.gradle.kotlin.dsl.withType


@Suppress("unused")
class HelperPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            apply<JavaPlugin>()
            apply<MavenPublishPlugin>()
            apply(plugin = "net.neoforged.gradle.userdev")

            val helperExtension = setupExtensions()
            setupConfigurations(helperExtension)
            setupAccessTransformers(helperExtension)
            setupModDependencies(helperExtension)
            setupPublishing(helperExtension)
            setupJava(helperExtension)
            setupRepositories()
            configureTasks(helperExtension)
            runs.all {
                workingDirectory.set(project.layout.projectDirectory.dir("run"))
                systemProperties.put("forge.logging.markers", "REGISTRIES")
                systemProperties.put("forge.logging.console.level", "debug")
                if (!helperExtension.runningInCI.getOrElse(false)) {
                    jvmArgument("-XX:+AllowEnhancedClassRedefinition")
                }
                modSources.add(project.sourceSets.main.get())
                systemProperties.put("terminal.ansi", "true")
            }
            minecraft.idea.useArgsFile.set(false)
        }
    }

    private fun Project.setupExtensions(): HelperExtension {
        val helperExtension = extensions.create<HelperExtension>("helper")
        group = helperExtension.projectGroup.get()
        version = helperExtension.fullVersion.get()
        base.archivesName.set(helperExtension.projectId)
        return helperExtension
    }

    private fun Project.setupConfigurations(helperExtension: HelperExtension) {
        // dependencies every mod source set (including api) compiles against
        val modCompileOnly = configurations.dependencyScope(MOD_COMPILE_ONLY)
        configurations.named(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME) {
            extendsFrom(modCompileOnly.get())
        }
        dependencies.addProvider(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, helperExtension.neoforge())
    }

    // feed the jar-relative paths used for the mods toml to neogradle as well
    private fun Project.setupAccessTransformers(helperExtension: HelperExtension) {
        minecraft.accessTransformers.files.from(helperExtension.accessTransformers.zip(sourceSets.main) { paths, main ->
            paths.mapNotNull { path -> main.resources.srcDirs.map { it.resolve(path) }.firstOrNull { it.isFile } }
        })
    }

    private fun Project.setupModDependencies(helperExtension: HelperExtension) {
        // makes the mod dependencies available as `mods` in the dependencies block
        dependencies.extensions.add(ModDependencyContainer::class.java, "mods", helperExtension.dependencies)
        wireModDependencies(helperExtension.dependencies)
    }

    private fun Project.setupJava(helperExtension: HelperExtension) = java {
        withSourcesJar()
        withJavadocJar()
        toolchain {
            languageVersion.set(helperExtension.java.version)
            vendor.set(helperExtension.java.vendor)
        }
    }

    private fun Project.setupPublishing(helperExtension: HelperExtension) = publishing {
        repositories.maven {
            if (helperExtension.maven.valid.getOrElse(false)) {
                url = helperExtension.maven.url.get()
                credentials {
                    username = helperExtension.maven.user.get()
                    password = helperExtension.maven.password.get()
                }
            } else {
                logger.info("Using repo folder")
                url = uri(layout.buildDirectory.dir("repo"))
            }
        }
        helperExtension.publication = publications.create<MavenPublication>(helperExtension.projectId.get() + "ToMaven") {
            groupId = helperExtension.projectGroup.get()
            artifactId = helperExtension.projectId.get()
            version = helperExtension.fullVersion.get()
            from(components.java)
            pom {
                name.set(helperExtension.projectName)
                url.set(helperExtension.projectUrl)
                packaging = "jar"
                scm {
                    connection.set(helperExtension.gitHub.connection)
                    developerConnection.set(helperExtension.gitHub.developerConnection)
                    url.set(helperExtension.gitHub.url)
                }
                if (helperExtension.gitHub.issuesUrl.isPresent) {
                    issueManagement {
                        system.set("github")
                        url.set(helperExtension.gitHub.issuesUrl)
                    }
                }
                if (helperExtension.gitHub.actionsUrl.isPresent) {
                    ciManagement {
                        system.set("github")
                        url.set(helperExtension.gitHub.actionsUrl)
                    }
                }
                licenses {
                    license {
                        name.set(helperExtension.license.name)
                        url.set(helperExtension.license.url)
                        distribution.set("repo")
                    }
                }
            }
        }
    }

    private fun Project.setupRepositories() = repositories {
        mavenLocal()
        mavenCentral()
        maven {
            name = "MinecraftschurliMods Maven"
            url = uri("https://maven.minecraftschurli.at/maven-public")
        }
    }

    private fun Project.configureTasks(helperExtension: HelperExtension) = tasks {
        this@configureTasks.createSetupGitHubActionsTask(helperExtension)
        withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
        }
        withType<JavaCompile>().matching { it.name == "neoFormRecompile" }.configureEach {
            options.compilerArgs.add("-Xlint:-removal")
        }
        javadoc {
            options.locale = "en_GB"
            // -locale only sets the html lang, the doclet's own texts follow the default locale of the javadoc jvm
            options.jFlags("-Duser.language=en", "-Duser.country=GB")
            // no generation date in the html, so the javadoc jar is reproducible
            (options as StandardJavadocDocletOptions).noTimestamp(true)
            options.encoding = "UTF-8"
            (options as CoreJavadocOptions).addStringOption("Xdoclint:all,-missing", "-public")
            (options as StandardJavadocDocletOptions).tags = listOf(
                "side:a:Side:",
                "apiNote:a:API Note:",
                "implSpec:a:Implementation Requirements:",
                "implNote:a:Implementation Note:"
            )
            if (JavaVersion.current().isJava9Compatible) {
                (options as CoreJavadocOptions).addBooleanOption("html5", true)
            }
        }
        withType<Jar>().configureEach {
            from(helperExtension.license.file)
            manifest.attributes(
                "Maven-Artifact"         to helperExtension.artifactLocator,
                "Specification-Title"    to helperExtension.projectId,
                "Specification-Vendor"   to helperExtension.projectVendor,
                "Specification-Version"  to "1",
                "Implementation-Title"   to helperExtension.projectId.zip(archiveClassifier.orElse("")) { id, c -> if (c.isNotEmpty()) "$id-$c" else id },
                "Implementation-Version" to helperExtension.projectVersion,
                "Implementation-Vendor"  to helperExtension.projectVendor,
                "Built-On-Java"          to "${System.getProperty("java.vm.version")} (${System.getProperty("java.vm.vendor")})",
                "Built-On-Minecraft"     to helperExtension.minecraftVersion,
                "Built-On-NeoForge"      to helperExtension.neoVersion,
                "FMLModType"             to helperExtension.projectType.map { it.modType },
                "LICENSE"                to helperExtension.license.name
            )
            exclude("**/*.psd")
            exclude("**/*.bbmodel")
        }
        processResources {
            doLast(MinifyJsonAction(outputs.files))
        }
        if (helperExtension.projectType.get() == HelperExtension.Type.MOD) {
            val generateFeatureFlagsFile = register<GenerateFeatureFlagsFileTask>("generateFeatureFlagsFile") {
                featureFlags.set(helperExtension.featureFlags.zip(helperExtension.projectId) { flags, modid -> flags.map { "$modid:$it" } })
            }
            val generateModsToml = register<GenerateModsTomlTask>("generateModsToml") {
                dependsOn(generateFeatureFlagsFile)

                val path = helperExtension.useLegacyModsToml.map { f -> if(f) "generated/modsToml/mods.toml" else "generated/modsToml/neoforge.mods.toml" }
                modsTomlFile.set(project.layout.buildDirectory.file(path))

                modsToml.set(helperExtension.toModsToml(project))
            }
            processResources {
                from(generateModsToml) {
                    into("META-INF/")
                }
                // the file is always generated, but only packaged (and referenced from the mods toml) if there are flags
                val noFeatureFlags = helperExtension.featureFlags.map { it.isEmpty() }.orElse(true)
                from(generateFeatureFlagsFile) {
                    into("META-INF/")
                    exclude { noFeatureFlags.get() }
                }
            }
        }
    }

    private fun Project.createSetupGitHubActionsTask(helperExtension: HelperExtension) {
        val githubOutputFile = providers.environmentVariable("GITHUB_OUTPUT")
        if (!githubOutputFile.isPresent) {
            logger.info("GITHUB_OUTPUT environment variable is not set, skipping setupGithubActions task registration")
            return
        }
        tasks.register<WriteGitHubActionsOutputTask>("setupGithubActions") {
            outputFile.set(project.file(githubOutputFile))
            values.put("modid", helperExtension.projectId)
            values.put("version", helperExtension.fullVersion)
            values.put("minecraft_version", helperExtension.minecraftVersion)
        }
    }
}
