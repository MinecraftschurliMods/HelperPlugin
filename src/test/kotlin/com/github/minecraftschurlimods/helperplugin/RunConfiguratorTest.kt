package com.github.minecraftschurlimods.helperplugin

import net.neoforged.gradle.dsl.common.extensions.Minecraft
import net.neoforged.gradle.dsl.common.runs.run.Run
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.getByType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class RunConfiguratorTest {
    @TempDir
    lateinit var dir: File

    private fun Run.modSourceSets(): Collection<SourceSet> = modSources.all().get().values()

    private fun Project.sourceSet(name: String) = sourceSets.getByName(name)

    @Test
    fun `test source set gets a junit run with the test sources`() {
        val project = modProject(dir)
        project.helper.withTestSourceSet()

        val junit = project.runs.getByName("junit")
        assertTrue(junit.isJUnit.get())
        assertTrue(project.sourceSet("test") in junit.unitTestSources.all().get().values())
        assertTrue(project.sourceSet("main") in junit.modSourceSets())
        assertFalse(project.sourceSet("test") in junit.modSourceSets())
    }

    @Test
    fun `game test sources only go to the game test server and client`() {
        val project = modProject(dir)
        project.helper.withCommonRuns()
        project.helper.withGameTestRuns()
        val gametest = project.sourceSet("gametest")

        val gameTestServer = project.runs.getByName("gameTestServer")
        val client = project.runs.getByName("client")
        val server = project.runs.getByName("server")
        assertTrue(gametest in gameTestServer.modSourceSets())
        assertTrue(gametest in client.modSourceSets())
        assertFalse(gametest in server.modSourceSets())
        assertEquals(MOD_ID, gameTestServer.systemProperties.get()["neoforge.enabledGameTestNamespaces"])
        assertEquals(MOD_ID, client.systemProperties.get()["neoforge.enabledGameTestNamespaces"])
        assertFalse("neoforge.enabledGameTestNamespaces" in server.systemProperties.get())
    }

    @Test
    fun `javadoc only covers the api source set`() {
        File(dir, "src/main/java/com/example").mkdirs()
        File(dir, "src/main/java/com/example/Impl.java").writeText("package com.example; class Impl {}")
        File(dir, "src/api/java/com/example").mkdirs()
        File(dir, "src/api/java/com/example/Api.java").writeText("package com.example; public interface Api {}")
        val project = modProject(dir)
        project.helper.withApiSourceSet()

        val javadoc = project.tasks.javadoc.get()
        assertEquals(setOf("Api.java"), javadoc.source.files.map { it.name }.toSet())
        assertSame(project.sourceSet("api").compileClasspath, javadoc.classpath)
    }

    @Test
    fun `access transformers are passed to NeoGradle`() {
        File(dir, "src/main/resources/META-INF").mkdirs()
        val at = File(dir, "src/main/resources/META-INF/accesstransformer.cfg").apply { writeText("") }
        val project = modProject(dir)
        project.helper.accessTransformers.set(listOf("META-INF/accesstransformer.cfg", "META-INF/missing.cfg"))

        val files = project.extensions.getByType<Minecraft>().accessTransformers.files.files
        assertEquals(setOf(at.canonicalFile), files.map { it.canonicalFile }.toSet())
    }
}
