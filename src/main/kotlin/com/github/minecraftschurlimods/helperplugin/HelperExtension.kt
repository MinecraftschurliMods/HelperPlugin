@file:Suppress("UnstableApiUsage")

package com.github.minecraftschurlimods.helperplugin

import com.github.minecraftschurlimods.helperplugin.moddependencies.ModDependency
import com.github.minecraftschurlimods.helperplugin.moddependencies.ModDependencyContainer
import net.neoforged.gradle.common.tasks.JarJar
import net.neoforged.gradle.dsl.common.runs.run.Run
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
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

    val runningInCI: Property<Boolean> = project.objects.property<Boolean>()
        .convention(project.providers.environmentVariable("CI").map { it.toBoolean() })
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
        .convention(project.localGradleProperty(projectType.map { when(it) { Type.MOD -> "mod.group"; Type.LIBRARY -> "lib.group" } }))
    val projectId: Property<String> = project.objects.property<String>()
        .convention(project.localGradleProperty(projectType.map { when(it) { Type.MOD -> "mod.id"; Type.LIBRARY -> "lib.name" } }))
    val projectVersion: Property<String> = project.objects.property<String>()
        .convention(project.localGradleProperty(projectType.map { when(it) { Type.MOD -> "mod.version"; Type.LIBRARY -> "lib.version" } }))
    val projectName: Property<String> = project.objects.property<String>()
        .convention(projectType.flatMap { when(it) { Type.MOD -> project.localGradleProperty("mod.name"); Type.LIBRARY -> projectId } })
    val projectCredits: Property<String> = project.objects.property<String>()
        .convention(project.localGradleProperty("mod.credits"))
    val projectAuthors: Property<String> = project.objects.property<String>()
        .convention(project.localGradleProperty("mod.authors"))
    val projectDescription: Property<String> = project.objects.property<String>()
        .convention(project.localGradleProperty("mod.description"))
    val projectVendor: Property<String> = project.objects.property<String>()
        .convention(project.localGradleProperty(projectType.map { when(it) { Type.MOD -> "mod.vendor"; Type.LIBRARY -> "lib.vendor" } }))
    val projectUrl: Property<String> = project.objects.property<String>()
        .convention(project.localGradleProperty(projectType.map { when(it) { Type.MOD -> "mod.url"; Type.LIBRARY -> "lib.url" } }).orElse(gitHub.url))
    val projectLogo: Property<String> = project.objects.property<String>()
        .convention(project.localGradleProperty("mod.logo"))
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
    val dependencies: ModDependencyContainer = project.objects.newInstance<ModDependencyContainer>()
    val modproperties: MapProperty<String, String> = project.objects.mapProperty()

    init {
        dependencies {
            required("neoforge") {
                versionRange.set(neoVersionRange)
                ordering.set(ModDependency.Ordering.NONE)
                side.set(ModDependency.Side.BOTH)
            }
            required("minecraft") {
                versionRange.set(minecraftVersionRange)
                ordering.set(ModDependency.Ordering.NONE)
                side.set(ModDependency.Side.BOTH)
            }
        }
    }

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

    private val neoforgeDependency = neoVersion.map { project.dependencyFactory.create("net.neoforged", "neoforge", it) }

    fun neoforge() = neoforgeDependency

    private lateinit var jarJar: TaskProvider<JarJar>
    fun withJarJar() {
        if (::jarJar.isInitialized) return
        val jar = project.tasks.named<Jar>("jar") {
            archiveClassifier.set("slim")
        }
        jarJar = project.tasks.named<JarJar>("jarJar") {
            archiveClassifier.set("")
            with(jar.get())
        }
        project.artifacts.add("archives", jarJar)
        publication.artifact(jarJar)
        project.jarJar.component(publication)
    }

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
        if (::jarJar.isInitialized) {
            jarJar {
                from(apiSourceSet.output)
            }
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
                if (runningInCI.getOrElse(false)) {
                    jvmArgument("-XX:+AllowEnhancedClassRedefinition")
                }
            }
            create("client")
            create("server") {
                singleInstance()
                programArgument("--nogui")
            }
        }
    }

    fun withDataGenRuns(cfg: Action<Run> = Action<Run>{}) {
        project.runs.create("data") {
            singleInstance()
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
            cfg.execute(this)
        }
        project.sourceSets.main {
            resources {
                srcDir(generatedResourcesDir)
                exclude(".cache")
            }
        }
    }

    fun withGameTestRuns(cfg: Action<Run> = Action<Run>{}) {
        project.runs.configureEach {
            if (name != "data") {
                systemProperties.put("forge.enabledGameTestNamespaces", projectId)
                if (::testSourceSet.isInitialized) {
                    modSource(testSourceSet)
                }
            }
        }
        project.runs.create("gameTestServer") {
            singleInstance()
            jvmArgument("-ea")
            cfg.execute(this)
        }
    }
}
