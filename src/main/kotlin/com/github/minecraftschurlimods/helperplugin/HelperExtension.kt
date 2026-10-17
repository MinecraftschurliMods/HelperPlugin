@file:Suppress("UnstableApiUsage")

package com.github.minecraftschurlimods.helperplugin

import com.github.minecraftschurlimods.helperplugin.moddependencies.ModDependency
import com.github.minecraftschurlimods.helperplugin.moddependencies.ModDependencyContainer
import net.neoforged.gradle.dsl.common.runs.run.Run
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Nested
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.setProperty
import java.net.URI
import javax.inject.Inject

// parses "1.21.4", "1.21", "26.1", "1.21.4-pre1" into (major, minor, patch); missing parts are null
private fun parseMcVersion(ver: String): List<Int?> {
    val parts = ver.split('.').flatMap { it.split('-') }
    return listOf(parts.getOrNull(0)?.toIntOrNull(), parts.getOrNull(1)?.toIntOrNull(), parts.getOrNull(2)?.toIntOrNull())
}

// if mcVer < 1.21.4 use a single "data" run, if mcVer >= 1.21.4 use separate "clientData" and "serverData" runs,
// as the data gen system changed in 1.21.4 and the old "data" run is no longer supported
// the versioning changed after 1.21.11 to continue with 26.1
internal fun usesSplitDataRuns(ver: String): Boolean {
    val (major, minor, patch) = parseMcVersion(ver)
    if (major == null) return false
    if (major > 1) return true
    if (minor == null) return false
    if (minor > 21) return true
    if (patch == null) return false
    return patch >= 4
}

// if mcVer <= 1.20.5 use mods.toml otherwise use neoforge.mods.toml
// the versioning changed after 1.21.11 to continue with 26.1
internal fun usesLegacyModsToml(ver: String): Boolean {
    val (major, minor, patch) = parseMcVersion(ver)
    if (major == null || major > 1) return false
    if (minor == null || minor > 20) return false
    if (patch == null) return true
    return patch <= 5
}

open class HelperExtension @Inject constructor(project: Project) {
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
    val featureFlags: ListProperty<String> = project.objects.listProperty<String>()
    val enumExtensions: Property<String> = project.objects.property<String>()
    val accessTransformers: SetProperty<String> = project.objects.setProperty<String>()
        .convention(project.provider {
            val default = "META-INF/accesstransformer.cfg"
            if (project.file("src/main/resources/$default").isFile) setOf(default) else emptySet()
        })
    val projectVendor: Property<String> = project.objects.property<String>()
        .convention(project.localGradleProperty(projectType.map { when(it) { Type.MOD -> "mod.vendor"; Type.LIBRARY -> "lib.vendor" } }))
    val projectUrl: Property<String> = project.objects.property<String>()
        .convention(project.localGradleProperty(projectType.map { when(it) { Type.MOD -> "mod.url"; Type.LIBRARY -> "lib.url" } }).orElse(gitHub.url))
    val projectLogo: Property<String> = project.objects.property<String>()
        .convention(project.localGradleProperty("mod.logo"))
    val projectNamespace: Property<String> = project.objects.property<String>()
    val projectUpdateJsonUrl: Property<String> = project.objects.property<String>()
        .convention(project.localGradleProperty("mod.update_url"))
    val projectModUrl: Property<String> = project.objects.property<String>()
        .convention(project.localGradleProperty("mod.mod_url"))
    val projectLogoBlur: Property<Boolean> = project.objects.property<Boolean>()
    val showAsResourcePack: Property<Boolean> = project.objects.property<Boolean>()
    val showAsDataPack: Property<Boolean> = project.objects.property<Boolean>()
    val services: SetProperty<String> = project.objects.setProperty<String>()
    val fileProperties: MapProperty<String, String> = project.objects.mapProperty()
    val features: MapProperty<String, String> = project.objects.mapProperty()
    val junitVersion: Property<String> = project.objects.property<String>()
        .convention(project.localGradleProperty("junit_version").orElse("5.13.4"))
    val mixins: NamedDomainObjectContainer<MixinConfig> = project.objects.domainObjectContainer(MixinConfig::class.java) { name -> MixinConfig(name, project) }
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
    val dependencies: ModDependencyContainer = ModDependencyContainer(project, minecraftVersion, runningInCI)
    val modproperties: MapProperty<String, String> = project.objects.mapProperty()

    internal val useSplitDataRuns = minecraftVersion.map(::usesSplitDataRuns)
    internal val useLegacyModsToml = minecraftVersion.map(::usesLegacyModsToml)

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

    class MixinConfig(val name: String, project: Project) {
        val config: Property<String> = project.objects.property<String>().convention(name)
        val requiredMods: SetProperty<String> = project.objects.setProperty<String>()
        val behaviorVersion: Property<String> = project.objects.property<String>()
    }

    enum class Type(val modType: String) {
        MOD("MOD"),
        LIBRARY("GAMELIBRARY")
    }

    private val neoforgeDependency = neoVersion.map { project.dependencyFactory.create("net.neoforged", "neoforge", it) }
    private val testframeworkDependency = neoVersion.map { project.dependencyFactory.create("net.neoforged", "testframework", it) }

    fun neoforge() = neoforgeDependency
    fun testframework() = testframeworkDependency

    private val runConfigurator by lazy { RunConfigurator(project, this) }

    fun mixin(config: String, cfg: Action<MixinConfig> = Action {}) = mixins.register(config, cfg)

    fun withJarJar() = runConfigurator.withJarJar()
    fun withApiSourceSet() = runConfigurator.withApiSourceSet()
    fun withTestSourceSet(cfg: Action<Run> = Action {}) = runConfigurator.withTestSourceSet(cfg)
    fun withGameTestSourceSet() = runConfigurator.withGameTestSourceSet()
    fun withDataGenSourceSet() = runConfigurator.withDataGenSourceSet()
    fun withCommonRuns() = runConfigurator.withCommonRuns()
    fun withDataGenRuns(cfg: Action<Run> = Action {}) = runConfigurator.withDataGenRuns(cfg)
    fun withGameTestRuns(cfg: Action<Run> = Action {}) = runConfigurator.withGameTestRuns(cfg)
}
