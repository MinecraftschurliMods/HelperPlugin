package com.github.minecraftschurlimods.helperplugin.moddependencies

import com.github.minecraftschurlimods.helperplugin.sourceSets
import net.neoforged.gradle.dsl.common.extensions.subsystems.Subsystems
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.getByType

/**
 * Adds the artifacts and repositories of all mod dependencies that declare an artifact.
 *
 * Per target source set (main unless [ModDependency.usedBy] is set):
 * - api: compileOnly(api artifact or artifact), api never gets runtime dependencies
 * - required: implementation(artifact), or compileOnly(api artifact) + runtimeOnly(artifact)
 * - optional: compileOnly(api artifact or artifact) + the source set's local run runtime(artifact) if [ModDependency.devRuntime]
 */
internal fun Project.wireModDependencies(dependencies: ModDependencyContainer) = afterEvaluate {
    dependencies.forEach { wireModDependency(it) }
}

internal fun Project.wireModDependency(dependency: ModDependency) {
    if (!dependency.artifact.isPresent) return
    val type = dependency.type.orNull ?: return
    if (type != ModDependency.Type.REQUIRED && type != ModDependency.Type.OPTIONAL) return
    val artifact = dependency.artifact
    val compileArtifact = dependency.apiArtifact.orElse(artifact)

    addModDependencyRepository(dependency)

    val targets = dependency.usedBy.getOrElse(emptySet()).ifEmpty { setOf(SourceSet.MAIN_SOURCE_SET_NAME) }
    for (target in targets) {
        val sourceSet = sourceSets.findByName(target)
            ?: throw IllegalArgumentException("Mod dependency ${dependency.name} is used by source set $target, which does not exist")
        if (sourceSet.name == "api") {
            this.dependencies.addProvider(sourceSet.compileOnlyConfigurationName, compileArtifact)
            continue
        }
        if (type == ModDependency.Type.REQUIRED) {
            if (dependency.apiArtifact.isPresent) {
                this.dependencies.addProvider(sourceSet.compileOnlyConfigurationName, compileArtifact)
                this.dependencies.addProvider(sourceSet.runtimeOnlyConfigurationName, artifact)
            } else {
                this.dependencies.addProvider(sourceSet.implementationConfigurationName, artifact)
            }
        } else {
            this.dependencies.addProvider(sourceSet.compileOnlyConfigurationName, compileArtifact)
            if (dependency.devRuntime.get()) {
                this.dependencies.addProvider(localRunRuntimeConfigurationName(sourceSet), artifact)
            }
        }
    }
}

/**
 * Adds the repository of a mod dependency. Called right after the dependency is declared, as NeoGradle resolves some
 * configurations (e.g. interfaceInjection) while the project is still being configured, and again when wiring for
 * dependencies that were registered lazily or got their repository later.
 * The content filter has to be known when the repository is created, gradle applies it immediately.
 */
internal fun Project.addModDependencyRepository(dependency: ModDependency) {
    val url = dependency.repository.orNull ?: return
    if (repositories.findByName(dependency.name) != null) return
    // the artifact can usually be resolved already (versions come from gradle.properties), without it the repository is unfiltered
    val group = dependency.repositoryGroup.orNull ?: dependency.artifact.orNull?.substringBefore(':')
    repositories.maven {
        name = dependency.name
        setUrl(url)
        if (group != null) {
            content { includeGroup(group) }
        }
    }
}

// the configuration neogradle adds to every run that has the source set as a mod source, named like ConfigurationUtils.getSourceSetName
private fun Project.localRunRuntimeConfigurationName(sourceSet: SourceSet): String {
    val postFix = extensions.getByType<Subsystems>().conventions.configurations.runRuntimeConfigurationPostFix.get()
    val prefix = if (sourceSet.name == SourceSet.MAIN_SOURCE_SET_NAME) "" else sourceSet.name.replaceFirstChar { it.uppercase() }
    val name = (prefix + postFix.replaceFirstChar { it.uppercase() }).replaceFirstChar { it.lowercase() }
    configurations.maybeCreate(name)
    return name
}
