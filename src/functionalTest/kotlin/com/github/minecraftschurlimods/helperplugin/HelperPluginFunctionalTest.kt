package com.github.minecraftschurlimods.helperplugin

import com.github.minecraftschurlimods.helperplugin.ModFixture.Companion.declaredDependencies
import com.github.minecraftschurlimods.helperplugin.ModFixture.Companion.report
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class HelperPluginFunctionalTest {
    @TempDir
    lateinit var dir: File

    private val fixture by lazy { ModFixture(dir) }

    @Test
    fun `generateModsToml writes the mods toml`() {
        fixture.file("src/main/resources/META-INF/accesstransformer.cfg").writeText("")
        fixture.buildScript(
            """
            helper.dependencies.jei { reason.set("integration") }
            helper.features.put("javaVersion", "[21,)")
            helper.mixin("examplemod.mixins.json") { requiredMods.add("jei") }
            """
        )
        fixture.run("generateModsToml")

        val toml = fixture.file("build/generated/modsToml/neoforge.mods.toml").readText()
        for (expected in listOf(
            "license = \"MIT\"",
            "modId = \"examplemod\"",
            "displayName = \"Example Mod\"",
            "[features.examplemod]",
            "requiredMods = [ \"jei\" ]",
            "[[dependencies.examplemod]]",
            "modId = \"jei\"",
            "reason = \"integration\"",
            "versionRange = \"[29.37,30)\"",
            "file = \"META-INF/accesstransformer.cfg\"",
        )) {
            assertTrue(expected in toml, "missing <$expected> in\n$toml")
        }
        assertTrue(toml.indexOf("license") < toml.indexOf("[[mods]]"), "scalars must precede tables")
    }

    @Test
    fun `stock test task is used without the api source set`() {
        fixture.buildScript("helper.withTestSourceSet()")
        val output = fixture.run("test", "--dry-run").output
        assertTrue(":test SKIPPED" in output, output)
        assertFalse(":testJunit" in output, output)
    }

    @Test
    fun `test forwards to testJunit with the api source set`() {
        fixture.buildScript(
            """
            helper.withApiSourceSet()
            helper.withTestSourceSet()
            """
        )
        val output = fixture.run("test", "--dry-run").output
        assertTrue(":testJunit SKIPPED" in output, output)
        assertTrue(":test SKIPPED" in output, output)
    }

    @Test
    fun `mod dependencies are wired into the configurations`() {
        fixture.buildScript(
            """
            helper.withApiSourceSet()
            helper.withDataGenSourceSet()

            helper.dependencies.jei { devRuntime.set(true) }
            helper.dependencies.required("geckolib") {
                artifact("com.geckolib:geckolib-neoforge-{mc}:{version}")
                repository("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/")
            }
            helper.dependencies.required("patchouli") {
                artifact("vazkii.patchouli:patchouli-neoforge:{version}", apiClassifier = "api")
            }

            dependencies {
                "modCompileOnly"("org.jetbrains:annotations:23.0.0")
                "apiCompileOnly"("at.minecraftschurli.mods:easydatagenlib:1.0:api")
                "dataImplementation"("at.minecraftschurli.mods:easydatagenlib:1.0")
            }

            """ + report(
                listOf("implementation", "compileOnly", "runtimeOnly", "localRunRuntime", "apiCompileOnly", "dataImplementation", "dataCompileOnly")
                    .associateWith { declaredDependencies(it) }
                    + ("repositories" to "repositories.map { it.name }")
            )
        )
        val result = fixture.run("help")

        val jeiApi = "mezz.jei:jei-26.1.2-common-api:29.37.0.99"
        val jei = "mezz.jei:jei-26.1.2-neoforge:29.37.0.99"
        val geckolib = "com.geckolib:geckolib-neoforge-26.1.2:5.5.2"
        val patchouliApi = "vazkii.patchouli:patchouli-neoforge:26.1-94:api"
        val patchouli = "vazkii.patchouli:patchouli-neoforge:26.1-94"
        val annotations = "org.jetbrains:annotations:23.0.0"
        val easyDatagenLibApi = "at.minecraftschurli.mods:easydatagenlib:1.0:api"
        val easyDatagenLib = "at.minecraftschurli.mods:easydatagenlib:1.0"

        assertContains(result.report("implementation"), geckolib)
        assertContains(result.report("compileOnly"), jeiApi, patchouliApi, annotations)
        assertContains(result.report("runtimeOnly"), patchouli)
        assertContains(result.report("localRunRuntime"), jei)
        assertContains(result.report("apiCompileOnly"), annotations, easyDatagenLibApi)
        assertContains(result.report("dataImplementation"), geckolib, easyDatagenLib)
        assertContains(result.report("dataCompileOnly"), jeiApi, patchouliApi, annotations)
        assertContains(result.report("repositories"), "jei", "geckolib")

        assertLacks(result.report("apiCompileOnly"), jeiApi, geckolib)
        assertLacks(result.report("implementation"), easyDatagenLib, jei)
        assertLacks(result.report("compileOnly"), easyDatagenLibApi)
        assertLacks(result.report("runtimeOnly"), jei)
    }

    @Test
    fun `access transformers and javadoc are configured`() {
        fixture.file("src/main/resources/META-INF/accesstransformer.cfg").writeText("")
        fixture.file("src/main/java/com/example/Impl.java").writeText("package com.example; class Impl {}")
        fixture.file("src/api/java/com/example/Api.java").writeText("package com.example; public interface Api {}")
        fixture.buildScript(
            """
            helper.withApiSourceSet()

            """ + report(mapOf(
                "accessTransformers" to "minecraft.accessTransformers.files.files.map { it.name }",
                "javadoc" to "tasks.javadoc.get().source.files.map { it.name }",
            ))
        )
        val result = fixture.run("help")

        assertTrue(result.report("accessTransformers") == "[accesstransformer.cfg]", result.report("accessTransformers"))
        assertTrue(result.report("javadoc") == "[Api.java]", result.report("javadoc"))
    }

    @Test
    fun `mod dependency repositories are available to configurations NeoGradle resolves while configuring`() {
        val repository = fixture.interfaceInjectionRepository("com.example.injected", "injected-lib", "1.0")
        fixture.buildScript(
            """
            dependencies {
                val injected = mods.required("injected") {
                    artifact("com.example.injected:injected-lib:{version}")
                    repository("$repository")
                }
                "interfaceInjection"(injected.artifact)
            }
            """
        )
        fixture.file("gradle.properties").appendText("dependency.injected.version=1.0\n")
        fixture.run("help")
    }

    private fun assertContains(report: String, vararg expected: String) {
        for (entry in expected) assertTrue(entry in report.removeSurrounding("[", "]").split(", "), "missing <$entry> in $report")
    }

    private fun assertLacks(report: String, vararg unexpected: String) {
        for (entry in unexpected) assertFalse(entry in report.removeSurrounding("[", "]").split(", "), "unexpected <$entry> in $report")
    }
}
