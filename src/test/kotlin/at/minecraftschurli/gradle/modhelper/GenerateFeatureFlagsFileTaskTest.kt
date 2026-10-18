package at.minecraftschurli.gradle.modhelper

import groovy.json.JsonSlurper
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.collections.get

class GenerateFeatureFlagsFileTaskTest {
    @TempDir
    lateinit var tempDir: File

    private fun task(): GenerateFeatureFlagsFileTask {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val task = project.tasks.register("gen", GenerateFeatureFlagsFileTask::class.java).get()
        task.featureFlagsFile.get().asFile.parentFile.mkdirs()
        return task
    }

    @Test
    fun emptyListWritesEmptyFlagsArray() {
        val task = task()
        task.featureFlags.set(emptyList())
        task.writeFeatureFlagsFile()
        assertEquals("{\"flags\":[]}", task.featureFlagsFile.asFile.get().readText())
    }

    @Test
    fun flagsAreWrittenInOrder() {
        val task = task()
        task.featureFlags.set(listOf("mod:a", "mod:b"))
        task.writeFeatureFlagsFile()
        assertEquals("{\"flags\":[\"mod:a\",\"mod:b\"]}", task.featureFlagsFile.asFile.get().readText())
    }

    @Test
    fun specialCharactersAreEscaped() {
        val original = "a\"b\\c"
        val task = task()
        task.featureFlags.set(listOf(original))
        task.writeFeatureFlagsFile()
        val text = task.featureFlagsFile.asFile.get().readText()
        val parsed = JsonSlurper().parseText(text) as Map<*, *>
        val flags = parsed["flags"] as List<*>
        assertEquals(original, flags[0])
    }
}
