package com.github.minecraftschurlimods.helperplugin

import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.gradle.testfixtures.ProjectBuilder
import java.io.File

internal const val MOD_ID = "examplemod"
internal const val MC_VERSION = "26.1.2"
internal const val NEO_VERSION = "26.1.2.99"

private val DEFAULT_PROPERTIES = mapOf(
    "mod.group" to "com.example",
    "mod.id" to MOD_ID,
    "mod.version" to "1.0.0",
    "mod.name" to "Example Mod",
    "mc_version" to MC_VERSION,
    "mc_version_range" to "[26.1,26.2)",
    "neo_version" to NEO_VERSION,
    "neo_version_range" to "[26.1.2.99,26.2)",
    "license.name" to "MIT",
    "github.owner" to "Example",
    "github.repo" to "example",
)

/**
 * A project with the helper plugin applied, configured through the same properties a mod's gradle.properties would set.
 * Not evaluated, so neither NeoGradle's nor the plugin's afterEvaluate hooks run.
 */
internal fun modProject(dir: File, properties: Map<String, String> = emptyMap()): Project {
    // a gradle user home outside the temp dir, as windows can't delete the files gradle keeps open in it
    val project = ProjectBuilder.builder().withProjectDir(dir).withGradleUserHomeDir(File("build/test-gradle-user-home").absoluteFile).build()
    (DEFAULT_PROPERTIES + properties).forEach { (key, value) -> project.extensions.extraProperties[key] = value }
    project.pluginManager.apply(HelperPlugin::class.java)
    return project
}

internal val Project.helper: HelperExtension get() = extensions.getByType()

// the configuration and all configurations it (transitively) extends from
internal fun Project.hierarchy(configuration: String): Set<String> = configurations.getByName(configuration).hierarchy.map { it.name }.toSet()
