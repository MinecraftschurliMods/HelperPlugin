package com.github.minecraftschurlimods.helperplugin

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class MinifyJsonActionTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun minifiesJsonFilesAndLeavesOtherFilesUntouched() {
        val a = File(tempDir, "a.json")
        a.writeText("{\n  \"x\": 1,\n  \"y\": [1, 2]\n}")
        val sub = File(tempDir, "sub").apply { mkdirs() }
        val b = File(sub, "b.json")
        b.writeText("{\n  \"nested\": {\n    \"ok\": true\n  }\n}\n")
        val c = File(tempDir, "c.txt")
        c.writeText("  keep   me  \n")

        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val task = project.tasks.register("t").get()
        MinifyJsonAction(project.files(tempDir)).execute(task)

        assertEquals("{\"x\":1,\"y\":[1,2]}", a.readText())
        val minifiedB = b.readText()
        assertFalse(minifiedB.any { it.isWhitespace() }, "expected no whitespace in '$minifiedB'")
        assertEquals("{\"nested\":{\"ok\":true}}", minifiedB)
        assertEquals("  keep   me  \n", c.readText())
    }
}
