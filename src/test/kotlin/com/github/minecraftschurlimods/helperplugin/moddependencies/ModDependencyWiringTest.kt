package com.github.minecraftschurlimods.helperplugin.moddependencies

import com.github.minecraftschurlimods.helperplugin.MC_VERSION
import com.github.minecraftschurlimods.helperplugin.helper
import com.github.minecraftschurlimods.helperplugin.modProject
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.URI

class ModDependencyWiringTest {
    @TempDir
    lateinit var dir: File
    private lateinit var project: Project

    @BeforeEach
    fun setUp() {
        project = modProject(dir, mapOf(
            "dependency.geckolib.version" to "5.5.2",
            "dependency.patchouli.version" to "94",
            "dependency.jei.version" to "29.5.0",
            "dependency.configured.version" to "1.0",
        ))
        project.helper.withApiSourceSet()
        project.helper.withDataGenSourceSet()
    }

    @Test
    fun `required dependency without api artifact goes to implementation`() {
        wire(project.helper.dependencies.required("geckolib") {
            artifact("com.geckolib:geckolib-neoforge-{mc}:{version}")
        })
        assertEquals(listOf("com.geckolib:geckolib-neoforge-$MC_VERSION:5.5.2"), declared("implementation"))
        assertEquals(emptyList<String>(), declared("compileOnly"))
        assertEquals(emptyList<String>(), declared("runtimeOnly"))
    }

    @Test
    fun `artifact can be built from a function`() {
        wire(project.helper.dependencies.required("geckolib") {
            artifact { mc, version -> "com.geckolib:geckolib-neoforge-$mc:$version" }
        })
        assertEquals(listOf("com.geckolib:geckolib-neoforge-$MC_VERSION:5.5.2"), declared("implementation"))
    }

    @Test
    fun `artifact can be built from a version only function with an api classifier`() {
        wire(project.helper.dependencies.required("patchouli") {
            artifact({ version -> "vazkii.patchouli:patchouli-neoforge:$version" }, "api")
        })
        assertEquals(listOf("vazkii.patchouli:patchouli-neoforge:94:api"), declared("compileOnly"))
        assertEquals(listOf("vazkii.patchouli:patchouli-neoforge:94"), declared("runtimeOnly"))
    }

    @Test
    fun `required dependency with api artifact compiles against api and runs with full jar`() {
        wire(project.helper.dependencies.required("patchouli") {
            artifact("vazkii.patchouli:patchouli-neoforge:{version}", apiClassifier = "api")
        })
        assertEquals(listOf("vazkii.patchouli:patchouli-neoforge:94:api"), declared("compileOnly"))
        assertEquals(listOf("vazkii.patchouli:patchouli-neoforge:94"), declared("runtimeOnly"))
        assertEquals(emptyList<String>(), declared("implementation"))
    }

    @Test
    fun `optional dependency compiles against api and is only on the run runtime`() {
        wire(project.helper.dependencies.jei { devRuntime.set(true) })
        assertEquals(listOf("mezz.jei:jei-$MC_VERSION-common-api:29.5.0"), declared("compileOnly"))
        assertEquals(listOf("mezz.jei:jei-$MC_VERSION-neoforge:29.5.0"), declared("localRunRuntime"))
        assertEquals(emptyList<String>(), declared("runtimeOnly"))
        assertEquals(emptyList<String>(), declared("implementation"))
    }

    @Test
    fun `optional dependency without dev runtime is compile only`() {
        wire(project.helper.dependencies.jei { devRuntime.set(false) })
        assertEquals(listOf("mezz.jei:jei-$MC_VERSION-common-api:29.5.0"), declared("compileOnly"))
        assertEquals(emptyList<String>(), declared("localRunRuntime"))
    }

    @Test
    fun `usedBy targets the given source sets and api only gets compile only`() {
        wire(project.helper.dependencies.required("geckolib") {
            artifact("com.geckolib:geckolib-neoforge-{mc}:{version}")
            usedBy("api", "data")
        })
        val geckolib = listOf("com.geckolib:geckolib-neoforge-$MC_VERSION:5.5.2")
        assertEquals(geckolib, declared("apiCompileOnly"))
        assertEquals(emptyList<String>(), declared("apiImplementation"))
        assertEquals(emptyList<String>(), declared("apiRuntimeOnly"))
        assertEquals(geckolib, declared("dataImplementation"))
        assertEquals(emptyList<String>(), declared("implementation"))
    }

    @Test
    fun `usedBy with an unknown source set fails`() {
        val dependency = project.helper.dependencies.required("geckolib") {
            artifact("com.geckolib:geckolib-neoforge-{mc}:{version}")
            usedBy("nope")
        }
        assertThrows(IllegalArgumentException::class.java) { wire(dependency) }
    }

    @Test
    fun `dependencies without an artifact or of other types are not wired`() {
        wire(project.helper.dependencies.optional("dummy"))
        val incompatible = project.helper.dependencies.incompatible("geckolib") {
            artifact("com.geckolib:geckolib-neoforge-{mc}:{version}")
        }
        wire(incompatible)
        for (configuration in listOf("implementation", "compileOnly", "runtimeOnly", "localRunRuntime")) {
            assertEquals(emptyList<String>(), declared(configuration), configuration)
        }
    }

    @Test
    fun `repository is added as soon as the dependency is declared`() {
        // NeoGradle resolves some configurations (e.g. interfaceInjection) before afterEvaluate, so no wiring here
        project.helper.dependencies.required("geckolib") {
            repository("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/")
            artifact("com.geckolib:geckolib-neoforge-{mc}:{version}")
        }
        val repository = project.repositories.findByName("geckolib") as MavenArtifactRepository?
        assertEquals(URI("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/"), repository?.url)
    }

    @Test
    fun `declared and wired dependency adds its repository once`() {
        wire(project.helper.dependencies.required("geckolib") {
            artifact("com.geckolib:geckolib-neoforge-{mc}:{version}")
            repository("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/")
        })
        assertEquals(1, project.repositories.filter { it.name == "geckolib" }.size)
    }

    @Test
    fun `repository set after declaring is added when wiring`() {
        // maybeCreate bypasses the create hook, the afterEvaluate wiring still adds the repository
        val dependency = project.helper.dependencies.maybeCreate("geckolib").apply {
            type.set(ModDependency.Type.REQUIRED)
            artifact("com.geckolib:geckolib-neoforge-{mc}:{version}")
            repository("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/")
        }
        wire(dependency)
        assertEquals(1, project.repositories.filter { it.name == "geckolib" }.size)
    }

    @Test
    fun `lazy registration is not supported`() {
        assertThrows(UnsupportedOperationException::class.java) { project.helper.dependencies.register("geckolib") }
    }

    @Test
    fun `presets declare their artifacts`() {
        val jei = project.helper.dependencies.jei()
        assertEquals("mezz.jei:jei-$MC_VERSION-neoforge:29.5.0", jei.artifact.get())
        assertEquals("mezz.jei:jei-$MC_VERSION-common-api:29.5.0", jei.apiArtifact.get())
        assertTrue(jei.repository.isPresent)
    }

    @Test
    fun `mod dependencies are available as mods in the dependencies block`() {
        val mods = (project.dependencies as org.gradle.api.plugins.ExtensionAware).extensions.getByName("mods")
        assertTrue(mods === project.helper.dependencies)
    }

    private fun wire(dependency: ModDependency) = project.wireModDependency(dependency)

    // declared external dependencies of a configuration (excluding inherited ones and neoforge itself), as group:name:version[:classifier]
    private fun declared(configuration: String): List<String> {
        val config = project.configurations.findByName(configuration) ?: return emptyList()
        return config.dependencies
            .filterIsInstance<ExternalModuleDependency>()
            .filter { it.name != "neoforge" }
            .map { listOfNotNull(it.group, it.name, it.version, it.artifacts.firstOrNull()?.classifier).joinToString(":") }
    }
}
