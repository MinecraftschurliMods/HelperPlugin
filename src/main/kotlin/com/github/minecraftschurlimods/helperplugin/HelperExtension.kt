@file:Suppress("UnstableApiUsage")

package com.github.minecraftschurlimods.helperplugin

import net.neoforged.gradle.util.TransformerUtils
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.kotlin.dsl.*
import java.net.URI
import javax.inject.Inject

open class HelperExtension @Inject constructor(private val project: Project) {
    lateinit var publication: MavenPublication

    @get:Nested
    val gitHub: GitHub = project.objects.newInstance(project)
    @get:Nested
    val license: License = project.objects.newInstance(project)
    @get:Nested
    val java: Java = project.objects.newInstance(project)
    @get:Nested
    val maven: Maven = project.objects.newInstance(project)
    @get:Nested
    val loader: Loader = project.objects.newInstance(project)
    @get:Nested
    val mcPublish: McPublish = project.objects.newInstance(project)


    val projectType: Property<Type> = project.objects.property<Type>()
        .convention(project.localGradleProperty("project_type").map { Type.valueOf(it) }.orElse(Type.MOD))
    val releaseType: Property<String> = project.objects.property<String>()
        .convention(project.providers.environmentVariable("RELEASE_TYPE").map {
            if ("snapshot".equals(it, ignoreCase = true)) {
                "SNAPSHOT"
            } else {
                it.lowercase()
            }
        }.orElse("SNAPSHOT"))
    val projectGroup: Property<String> = project.objects.property<String>()
        .convention(project.localGradleProperty(projectType.map { when(it) { Type.MOD -> "mod_group"; Type.LIBRARY -> "lib_group" } }))
    val projectId: Property<String> = project.objects.property<String>()
        .convention(project.localGradleProperty(projectType.map { when(it) { Type.MOD -> "mod_id"; Type.LIBRARY -> "lib_name" } }))
    val projectVersion: Property<String> = project.objects.property<String>()
        .convention(project.localGradleProperty(projectType.map { when(it) { Type.MOD -> "mod_version"; Type.LIBRARY -> "lib_version" } }))
    val projectName: Property<String> = project.objects.property<String>()
        .convention(projectType.flatMap { when(it) { Type.MOD -> project.localGradleProperty("mod_name"); Type.LIBRARY -> projectId } })
    val projectAuthors: Property<String> = project.objects.property<String>()
        .convention(project.localGradleProperty("mod_authors"))
    val projectDescription: Property<String> = project.objects.property<String>()
        .convention(project.localGradleProperty("mod_description"))
    val projectVendor: Property<String> = project.objects.property<String>()
        .convention(project.localGradleProperty("vendor"))
    val projectUrl: Property<String> = project.objects.property<String>()
        .convention(project.localGradleProperty("url").orElse(gitHub.url))
    val minecraftVersion: Property<String> = project.objects.property<String>()
        .convention(project.localGradleProperty("mc_version"))
    val minecraftVersionRange: Property<String> = project.objects.property<String>()
        .convention(project.localGradleProperty("mc_version_range"))
    val neoVersion: Property<String> = project.objects.property<String>()
        .convention(project.localGradleProperty("neo_version"))
    val neoVersionRange: Property<String> = project.objects.property<String>()
        .convention(project.localGradleProperty("neo_version_range"))
    val fullVersion: Property<String> = project.objects.property<String>()
        .convention(minecraftVersion.zip(projectVersion) { mc, proj -> "$mc-$proj" }.zip(releaseType) { version, rel -> if ("release" == rel) version else "$version-$rel" })
    val artifactLocator: Property<String> = project.objects.property<String>()
        .convention(projectGroup.zip(projectId) { group, id -> "$group:$id" }.zip(fullVersion) { selector, version -> "$selector:$version" })
    val generatedResourcesDir: DirectoryProperty = project.objects.directoryProperty()
        .convention(project.layout.projectDirectory.dir("src/main/generated"))
    val dependencies: ListProperty<Dependency> = project.objects.listProperty<Dependency>()
        .value(projectType.map(TransformerUtils.guard { if (it == Type.MOD) listOf(
            Dependency(
                "neoforge",
                neoVersionRange.get(),
                "required"
            ),
            Dependency(
                "minecraft",
                minecraftVersionRange.get(),
                "required"
            )
        ) else null }))
    val modproperties: MapProperty<String, String> = project.objects.mapProperty()

    fun dependency(modId: String, versionRange: String, type: String, ordering: String = "NONE", side: String = "BOTH") = dependencies.add(Dependency(modId, versionRange, type, ordering, side))

    open class GitHub @Inject constructor(project: Project) {
        val owner: Property<String> = project.objects.property<String>()
            .convention(project.localGradleProperty("github.owner"))
        val repository: Property<String> = project.objects.property<String>()
            .convention(project.localGradleProperty("github.repo"))
        val ref: Property<String> = project.objects.property<String>()
            .convention(owner.zip(repository) { owner, repo -> "$owner/$repo" })
        val url: Property<String> = project.objects.property<String>()
            .convention(ref.map { "https://github.com/$it" })
        val issuesUrl: Property<String> = project.objects.property<String>()
            .convention(url.map { "$it/issues" })
        val actionsUrl: Property<String> = project.objects.property<String>()
            .convention(url.map { "$it/actions" })
        val connection: Property<String> = project.objects.property<String>()
            .convention(ref.map { "scm:git:git://github.com/$it.git" })
        val developerConnection: Property<String> = project.objects.property<String>()
            .convention(ref.map { "scm:git:git@github.com:$it.git" })
    }

    open class License @Inject constructor(project: Project) {
        val file: Property<String> = project.objects.property<String>()
            .convention(project.localGradleProperty("license.file").orElse("LICENSE"))
        val name: Property<String> = project.objects.property<String>()
            .convention(project.localGradleProperty("license.name"))
        val url: Property<String> = project.objects.property<String>()
            .convention(project.localGradleProperty("license.url"))
    }

    open class Java @Inject constructor(project: Project) {
        val version: Property<JavaLanguageVersion> = project.objects.property<JavaLanguageVersion>()
            .convention(project.localGradleProperty("java_version").map { JavaLanguageVersion.of(it) })
        val vendor: Property<JvmVendorSpec> = project.objects.property<JvmVendorSpec>()
            .convention(project.providers.environmentVariable("CI").map { it.toBoolean() }.orElse(false).map { if (it) JvmVendorSpec.ADOPTIUM else JvmVendorSpec.JETBRAINS })
    }

    open class Maven @Inject constructor(project: Project) {
        val url: Property<URI> = project.objects.property<URI>()
            .convention(project.providers.environmentVariable("MAVEN_URL").map { project.uri(it) })
        val user: Property<String> = project.objects.property<String>()
            .convention(project.providers.environmentVariable("MAVEN_USER"))
        val password: Property<String> = project.objects.property<String>()
            .convention(project.providers.environmentVariable("MAVEN_PASSWORD"))
        val valid: Provider<Boolean> = url.zip(user.zip(password) { _, _ -> true }) { _, _ -> true }.orElse(false)
    }

    open class Loader @Inject constructor(project: Project) {
        val name: Property<String> = project.objects.property<String>()
            .convention(project.localGradleProperty("loader.name").orElse("javafml"))
        val version: Property<String> = project.objects.property<String>()
            .convention(project.localGradleProperty("loader.version"))
    }

    open class McPublish @Inject constructor(project: Project) {
        val curseforge: Property<Int> = project.objects.property<Int>()
            .convention(project.localGradleProperty("mc-publish.curseforge").map { it.toInt() })
        val modrinth: Property<String> = project.objects.property<String>()
            .convention(project.localGradleProperty("mc-publish.modrinth"))
    }

    enum class Type(val modType: String) {
        MOD("MOD"),
        LIBRARY("GAMELIBRARY")
    }

    private val neoforgeDependency by lazy { project.dependencyFactory.create("net.neoforged", "neoforge", neoVersion.get())/*.apply {
        version {
            strictly(neoVersionRange.get())
            prefer(neoVersion.get())
        }
    }*/}

    fun neoforge() = neoforgeDependency

    private lateinit var apiSourceSet: SourceSet
    fun withApiSourceSet() {
        if (::apiSourceSet.isInitialized) return
        apiSourceSet = project.sourceSets.maybeCreate("api")
        project.dependencies {
            apiSourceSet.compileOnlyConfigurationName(neoforge())
            "implementation"(apiSourceSet.output)
            if (::testSourceSet.isInitialized) {
                testSourceSet.implementationConfigurationName(apiSourceSet.output)
            }
            if (::dataGenSourceSet.isInitialized) {
                dataGenSourceSet.implementationConfigurationName(apiSourceSet.output)
            }
        }
        project.tasks.jar {
            from(apiSourceSet.output)
        }
        project.tasks.sourcesJar {
            from(apiSourceSet.allSource)
        }
        val apiJar = project.tasks.register<Jar>("apiJar") {
            dependsOn(apiSourceSet.classesTaskName)
            archiveClassifier.set("api")
            from(apiSourceSet.allSource)
            from(apiSourceSet.output)
        }
        project.artifacts.add("archives", apiJar)
        publication.artifact(apiJar)
        project.runs.configureEach { modSource(apiSourceSet) }
    }

    private lateinit var testSourceSet: SourceSet
    fun withTestSourceSet() {
        if (::testSourceSet.isInitialized) return
        testSourceSet = project.sourceSets.maybeCreate("test")
        project.dependencies {
            testSourceSet.implementationConfigurationName(project.sourceSets.main.output)
            testSourceSet.implementationConfigurationName(neoforge())
            if (::apiSourceSet.isInitialized) {
                testSourceSet.implementationConfigurationName(apiSourceSet.output)
            }
        }
        project.runs.findByName("gameTestServer")?.modSource(testSourceSet)
    }

    private lateinit var dataGenSourceSet: SourceSet
    fun withDataGenSourceSet() {
        if (::dataGenSourceSet.isInitialized) return
        dataGenSourceSet = project.sourceSets.maybeCreate("data")
        project.dependencies {
            dataGenSourceSet.implementationConfigurationName(project.sourceSets.main.output)
            dataGenSourceSet.implementationConfigurationName(neoforge())
            if (::apiSourceSet.isInitialized) {
                dataGenSourceSet.implementationConfigurationName(apiSourceSet.output)
            }
        }
        project.runs.findByName("data")?.modSource(dataGenSourceSet)
    }

    fun withCommonRuns() {
        project.runs {
            configureEach {
                workingDirectory.set(project.layout.projectDirectory.dir("run"))
                systemProperties.put("forge.logging.markers", "REGISTRIES")
                systemProperties.put("forge.logging.console.level", "debug")
                modSources.add(project.sourceSets.main)
            }
            create("client")
            create("server") {
                programArgument("--nogui")
            }
        }
    }

    fun withDataGenRuns() {
        project.runs.create("data") {
            if (::dataGenSourceSet.isInitialized) {
                modSource(dataGenSourceSet)
            }
            programArguments.add("--mod")
            programArguments.add(projectId)
            programArguments.add("--all")
            programArguments.add("--output")
            programArguments.add(generatedResourcesDir.map { it.asFile.absolutePath })
            programArguments.add("--existing")
            programArguments.add(project.layout.projectDirectory.dir("src/main/resources/").asFile.absolutePath)
        }
        project.sourceSets.main.configure {
            resources.srcDir(generatedResourcesDir)
        }
    }

    fun withGameTestRuns() {
        project.runs.configureEach {
            if (name != "data") {
                systemProperties.put("forge.enabledGameTestNamespaces", projectId)
                if (::testSourceSet.isInitialized) {
                    modSource(testSourceSet)
                }
            }
        }
        project.runs.create("gameTestServer")
    }
}
