package com.github.minecraftschurlimods.helperplugin.modstoml

import com.github.minecraftschurlimods.helperplugin.MOD_ID
import com.github.minecraftschurlimods.helperplugin.helper
import com.github.minecraftschurlimods.helperplugin.modProject
import com.github.minecraftschurlimods.helperplugin.moddependencies.ModDependency
import org.gradle.api.Project
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ModsTomlFactoryTest {
    @TempDir
    lateinit var dir: File

    private fun Project.toml() = helper.toModsToml(this).get()

    @Test
    fun `basic metadata comes from the gradle properties`() {
        val toml = modProject(dir).toml()
        assertEquals("MIT", toml.license)
        assertEquals("https://github.com/Example/example/issues", toml.issueTrackerURL)
        val mod = toml.mods.single()
        assertEquals(MOD_ID, mod.modId)
        assertEquals("1.0.0", mod.version)
        assertEquals("Example Mod", mod.displayName)
    }

    @Test
    fun `loaderVersion is only written together with modLoader`() {
        val project = modProject(dir, mapOf("loader.version" to "[4,)"))
        assertEquals("javafml", project.toml().modLoader)
        assertEquals("[4,)", project.toml().loaderVersion)

        project.helper.loader.name.convention(null as String?)
        project.helper.loader.name.set(null as String?)
        assertNull(project.toml().modLoader)
        assertNull(project.toml().loaderVersion)
    }

    @Test
    fun `empty collections are omitted`() {
        val toml = modProject(dir).toml()
        assertNull(toml.services)
        assertNull(toml.properties)
        assertNull(toml.features)
        assertNull(toml.mixins)
        assertNull(toml.modproperties)
        assertNull(toml.accessTransformers)
        assertNull(toml.mods.single().featureFlags)
    }

    @Test
    fun `default access transformer is picked up when the file exists`() {
        File(dir, "src/main/resources/META-INF").mkdirs()
        File(dir, "src/main/resources/META-INF/accesstransformer.cfg").writeText("")
        assertEquals(listOf(AccessTransformer("META-INF/accesstransformer.cfg")), modProject(dir).toml().accessTransformers)
    }

    @Test
    fun `features, feature flags and mixins are mapped`() {
        val project = modProject(dir)
        project.helper.features.put("javaVersion", "[21,)")
        project.helper.featureFlags.add("wip")
        project.helper.mixin("a.mixins.json") {
            requiredMods.add("jei")
            behaviorVersion.set("0.8")
        }
        project.helper.mixin("b.mixins.json")

        val toml = project.toml()
        assertEquals(mapOf(MOD_ID to mapOf("javaVersion" to "[21,)")), toml.features)
        assertEquals("META-INF/feature_flags.json", toml.mods.single().featureFlags)
        assertEquals(listOf(Mixin("a.mixins.json", listOf("jei"), "0.8"), Mixin("b.mixins.json", null, null)), toml.mixins)
    }

    @Test
    fun `dependencies are keyed by the mod id`() {
        val project = modProject(dir, mapOf("dependency.jei.version.range" to "[29,30)"))
        project.helper.dependencies.jei {
            reason.set("integration")
            ordering.set(ModDependency.Ordering.AFTER)
        }

        val dependencies = project.toml().dependencies!!.getValue(MOD_ID).associateBy { it.modId }
        assertEquals(setOf("neoforge", "minecraft", "jei"), dependencies.keys)
        assertEquals(Dependency("neoforge", "required", null, "[26.1.2.99,26.2)", "NONE", "BOTH", null, null), dependencies["neoforge"])
        assertEquals(Dependency("jei", "optional", "integration", "[29,30)", "AFTER", null, null, null), dependencies["jei"])
    }
}
