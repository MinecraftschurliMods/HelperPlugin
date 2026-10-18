package com.github.minecraftschurlimods.helperplugin

import org.gradle.api.Project
import org.gradle.api.artifacts.FileCollectionDependency
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File

class DependencyLayeringTest {
    @TempDir
    lateinit var dir: File

    @Test
    fun `main compileOnly extends modCompileOnly`() {
        val project = modProject(dir)
        assertTrue(MOD_COMPILE_ONLY in project.hierarchy("compileOnly"))
    }

    @Test
    fun `api only inherits modCompileOnly and nothing from main`() {
        val project = modProject(dir)
        project.helper.withApiSourceSet()
        val api = project.sourceSets.getByName("api")

        val compileOnly = project.hierarchy(api.compileOnlyConfigurationName)
        assertTrue(MOD_COMPILE_ONLY in compileOnly)
        assertFalse("compileOnly" in compileOnly)
        assertFalse("implementation" in project.hierarchy(api.implementationConfigurationName))
        assertFalse("runtimeOnly" in project.hierarchy(api.runtimeOnlyConfigurationName))
    }

    @Test
    fun `api feature capability is named after the artifact`() {
        val project = modProject(dir)
        project.helper.withApiSourceSet()
        for (configuration in listOf("apiApiElements", "apiRuntimeElements")) {
            val capabilities = project.configurations.getByName(configuration).outgoing.capabilities
            assertEquals(listOf("com.example:$MOD_ID-api:${project.version}"), capabilities.map { "${it.group}:${it.name}:${it.version}" }, configuration)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["test", "data", "gametest"])
    fun `derived source sets inherit from main`(name: String) {
        val project = modProject(dir)
        createSourceSet(project, name)
        val sourceSet = project.sourceSets.getByName(name)

        assertTrue("implementation" in project.hierarchy(sourceSet.implementationConfigurationName))
        assertTrue("runtimeOnly" in project.hierarchy(sourceSet.runtimeOnlyConfigurationName))
        val compileOnly = project.hierarchy(sourceSet.compileOnlyConfigurationName)
        assertTrue("compileOnly" in compileOnly)
        assertTrue(MOD_COMPILE_ONLY in compileOnly)
    }

    @Test
    fun `main does not inherit from derived source sets`() {
        val project = modProject(dir)
        project.helper.withDataGenSourceSet()
        assertFalse("dataImplementation" in project.hierarchy("implementation"))
        assertFalse("dataCompileOnly" in project.hierarchy("compileOnly"))
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `api output reaches derived source sets regardless of call order`(apiFirst: Boolean) {
        val project = modProject(dir)
        if (apiFirst) project.helper.withApiSourceSet()
        project.helper.withDataGenSourceSet()
        if (!apiFirst) project.helper.withApiSourceSet()
        val apiOutput = project.sourceSets.getByName("api").output.files

        val dataFiles = project.configurations.getByName("dataImplementation").allDependencies
            .filterIsInstance<FileCollectionDependency>()
            .flatMap { it.files.files }
        assertTrue(dataFiles.containsAll(apiOutput))
    }

    private fun createSourceSet(project: Project, name: String) = when (name) {
        "test" -> project.helper.withTestSourceSet()
        "data" -> project.helper.withDataGenSourceSet()
        "gametest" -> project.helper.withGameTestSourceSet()
        else -> error(name)
    }
}
