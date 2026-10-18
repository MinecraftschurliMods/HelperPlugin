package at.minecraftschurli.gradle.modhelper

import net.neoforged.gradle.dsl.common.extensions.Minecraft
import net.neoforged.gradle.dsl.common.runs.run.Run
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.external.javadoc.StandardJavadocDocletOptions
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
    fun `game test sources go to the client and server runs but not the data runs`() {
        val project = modProject(dir)
        project.helper.withCommonRuns()
        val gametest = project.sourceSet("gametest")

        for (name in listOf("gameTestServer", "client", "server")) {
            val run = project.runs.getByName(name)
            assertTrue(gametest in run.modSourceSets(), name)
            assertEquals(MOD_ID, run.systemProperties.get()["neoforge.enabledGameTestNamespaces"], name)
        }
        val data = project.runs.getByName("clientData")
        assertFalse(gametest in data.modSourceSets())
        assertFalse("neoforge.enabledGameTestNamespaces" in data.systemProperties.get())
    }

    @Test
    fun `run functions called after withCommonRuns only add their configuration`() {
        val project = modProject(dir)
        project.helper.withCommonRuns()
        project.helper.withDataGenRuns { arguments.add("--existing-mod") }
        project.helper.withGameTestRuns { jvmArgument("-Dexample=true") }

        val data = project.runs.getByName("clientData").arguments.get()
        assertEquals(1, data.count { it == "--mod" }, data.toString())
        assertTrue("--existing-mod" in data, data.toString())
        val gameTest = project.runs.getByName("gameTestServer").jvmArguments.get()
        assertEquals(1, gameTest.count { it == "-ea" }, gameTest.toString())
        assertTrue("-Dexample=true" in gameTest, gameTest.toString())
        val gameTestArguments = project.runs.getByName("gameTestServer").arguments.get()
        assertEquals(1, gameTestArguments.count { it == "--nogui" }, gameTestArguments.toString())
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
    fun `javadoc is generated in english without timestamp`() {
        val options = modProject(dir).tasks.javadoc.get().options
        assertEquals("en_GB", options.locale)
        assertTrue(options.jFlags.orEmpty().containsAll(listOf("-Duser.language=en", "-Duser.country=GB")), options.jFlags.toString())
        assertTrue((options as StandardJavadocDocletOptions).isNoTimestamp)
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
