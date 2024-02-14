@file:Suppress("UnstableApiUsage")

package com.github.minecraftschurlimods.helperplugin

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Nested
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.the
import java.net.URI
import javax.inject.Inject

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
    val projectVendor: Property<String> = project.objects.property<String>()
        .convention(project.localGradleProperty(projectType.map { when(it) { Type.MOD -> "mod_author"; Type.LIBRARY -> "vendor" } }))
    val projectUrl: Property<String> = project.objects.property<String>()
        .convention(project.localGradleProperty("url").orElse(gitHub.url))
    val minecraftVersion: Property<String> = project.objects.property<String>()
        .convention(project.localGradleProperty("mc_version"))
    val neoVersion: Property<String> = project.objects.property<String>()
        .convention(project.localGradleProperty("neo_version"))
    val neoDependency: Property<String> = project.objects.property<String>()
        .convention(neoVersion.map { "net.neoforged:neoforge:$it" })
    val fullVersion: Property<String> = project.objects.property<String>()
        .convention(minecraftVersion.zip(projectVersion) { mc, proj -> "$mc-$proj" }.zip(releaseType) { version, rel -> if ("release" == rel) version else "$version-$rel" })
    val artifactLocator: Property<String> = project.objects.property<String>()
        .convention(projectGroup.zip(projectId) { group, id -> "$group:$id" }.zip(fullVersion) { selector, version -> "$selector:$version" })


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
            .convention(project.localGradleProperty("license.file"))
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

    enum class Type(val modType: String) {
        MOD("MOD"),
        LIBRARY("GAMELIBRARY")
    }

    private val neoforgeDependency = neoDependency.map { project.dependencyFactory.create(it) }

    fun neoforge() = neoforgeDependency
}
