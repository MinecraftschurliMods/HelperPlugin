package com.github.minecraftschurlimods.helperplugin

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.MapProperty
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.CoreJavadocOptions
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.kotlin.dsl.*
import java.time.Instant
import java.time.format.DateTimeFormatter


class HelperPlugin : Plugin<Project> {    
    override fun apply(project: Project) {
        project.apply<JavaPlugin>()
        project.apply<MavenPublishPlugin>()
        project.apply(plugin = "net.neoforged.gradle.userdev")

        val helperExtension = project.setupExtensions()
        project.setupRepositories()
        project.tasks.configureTasks(helperExtension)
        project.setupArtifacts()
    }

    private fun Project.setupExtensions(): HelperExtension {
        val helperExtension = extensions.create<HelperExtension>("helper")
        group = helperExtension.projectGroup.get()
        version = helperExtension.fullVersion.get()
        base.archivesName.set(helperExtension.projectId)
        java.withSourcesJar()
        java.withJavadocJar()
        java.toolchain {
            languageVersion.set(helperExtension.java.version)
            vendor.set(helperExtension.java.vendor)
        }
        publishing.repositories.maven {
            if (helperExtension.maven.valid.getOrElse(false)) {
                url = helperExtension.maven.url.get()
                credentials {
                    username = helperExtension.maven.user.get()
                    password = helperExtension.maven.password.get()
                }
            } else {
                println("Using repo folder")
                url = uri(layout.buildDirectory.dir("repo"))
            }
        }
        helperExtension.publication = publishing.publications.create<MavenPublication>(helperExtension.projectId.get() + "ToMaven") {
            groupId = helperExtension.projectGroup.get()
            artifactId = helperExtension.projectId.get()
            version = helperExtension.fullVersion.get()
            from(components.getByName("java"))
            pom {
                name.set(this@setupExtensions.name)
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
        return helperExtension
    }

    private fun Project.setupRepositories() = repositories {
        mavenLocal()
        mavenCentral()
        maven {
            name = "Minecraftschurli Maven"
            url = uri("https://minecraftschurli.ddns.net/repository/maven-public")
        }
    }

    private fun TaskContainer.configureTasks(helperExtension: HelperExtension) {
        createSetupGitHubActionsTask(helperExtension)
        withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
        }
        named<Javadoc>("javadoc").configure {
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
            val extension = archiveClassifier.map { if (it.isNotEmpty()) "-$it" else "" }.getOrElse("")
            manifest.attributes(
                "Maven-Artifact"         to helperExtension.artifactLocator.get(),
                "Specification-Title"    to helperExtension.projectId.get(),
                "Specification-Vendor"   to helperExtension.projectVendor.get(),
                "Specification-Version"  to "1",
                "Implementation-Title"   to helperExtension.projectId.get() + extension,
                "Implementation-Version" to helperExtension.projectVersion.get(),
                "Implementation-Vendor"  to helperExtension.projectVendor.get(),
                "Built-On-Java"          to "${System.getProperty("java.vm.version")} (${System.getProperty("java.vm.vendor")})",
                "Built-On-Minecraft"     to helperExtension.minecraftVersion.get(),
                "Built-On-NeoForge"      to helperExtension.neoVersion.get(),
                "Timestamp"              to DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                "FMLModType"             to helperExtension.projectType.map { it.modType }.get(),
                "LICENSE"                to helperExtension.license.name.get()
            )
        }
    }

    private fun TaskContainer.createSetupGitHubActionsTask(helperExtension: HelperExtension) {
        val githubOutputFile = System.getenv("GITHUB_OUTPUT") ?: return
        register<WriteGitHubActionsOutputTask>("setupGithubActions") {
            outputFile.set(project.file(githubOutputFile))
            values.put("modid", helperExtension.projectId)
            values.put("version", helperExtension.fullVersion)
            values.put("minecraft_version", helperExtension.minecraftVersion)
        }
    }

    private fun Project.setupArtifacts() = artifacts {
        add("archives", tasks.named<Jar>("jar"))
        add("archives", tasks.named<Jar>("sourcesJar"))
        add("archives", tasks.named<Jar>("javadocJar"))
    }

    private val Project.publishing: PublishingExtension
        get() = this.the<PublishingExtension>()

    private val Project.java: JavaPluginExtension
        get() = this.the<JavaPluginExtension>()

    private val Project.base: BasePluginExtension
        get() = this.the<BasePluginExtension>()
}

