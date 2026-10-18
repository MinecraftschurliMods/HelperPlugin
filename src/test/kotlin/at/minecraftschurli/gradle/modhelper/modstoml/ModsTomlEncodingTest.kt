package at.minecraftschurli.gradle.modhelper.modstoml

import com.akuleshov7.ktoml.Toml
import kotlinx.serialization.encodeToString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ModsTomlEncodingTest {
    private fun fullModel() = ModsToml(
        modLoader = null,
        loaderVersion = null,
        license = "MIT",
        issueTrackerURL = "https://x/issues",
        showAsResourcePack = false,
        showAsDataPack = true,
        services = listOf("a.B", "c.D"),
        properties = mapOf("k" to "v"),
        mods = listOf(
            Mod(
                "examplemod", null, "1.0", "Example", "https://x", null, null, "logo.png", false, "me", "you",
                "Line one\nLine \"two\"", "META-INF/feature_flags.json", null
            )
        ),
        features = mapOf("examplemod" to mapOf("javaVersion" to "[21,)")),
        mixins = listOf(Mixin("a.mixins.json", listOf("jei", "jade"), "0.8"), Mixin("b.mixins.json", null, null)),
        mcPublish = McPublish("abc", 123),
        dependencies = mapOf(
            "examplemod" to listOf(
                Dependency("neoforge", "[21,)", "required", null, "NONE", "BOTH", null, null),
                Dependency(
                    "jei",
                    "[1,)",
                    "optional",
                    "integration",
                    null,
                    null,
                    null,
                    DependencyMcPublish("jei", "jei")
                ),
            )
        ),
        modproperties = mapOf("examplemod" to mapOf("p" to "q")),
        accessTransformers = listOf(AccessTransformer("META-INF/accesstransformer.cfg")),
    )

    @Test
    fun fullModelMatchesGoldenOutput() {
        val expected = """
license = "MIT"
issueTrackerURL = "https://x/issues"
showAsResourcePack = false
showAsDataPack = true
services = [ "a.B", "c.D" ]

[properties]
    k = "v"

[[mods]]
    modId = "examplemod"
    version = "1.0"
    displayName = "Example"
    displayURL = "https://x"
    logoFile = "logo.png"
    logoBlur = false
    credits = "me"
    authors = "you"
    description = '''
Line one
Line "two"
'''
    featureFlags = "META-INF/feature_flags.json"

[features.examplemod]
    javaVersion = "[21,)"

[[mixins]]
    config = "a.mixins.json"
    requiredMods = [ "jei", "jade" ]
    behaviorVersion = "0.8"

[[mixins]]
    config = "b.mixins.json"

[mc-publish]
    modrinth = "abc"
    curseforge = 123

[[dependencies.examplemod]]
    modId = "neoforge"
    versionRange = "[21,)"
    type = "required"
    ordering = "NONE"
    side = "BOTH"

[[dependencies.examplemod]]
    modId = "jei"
    versionRange = "[1,)"
    type = "optional"
    reason = "integration"

    [dependencies.examplemod.mc-publish]
        modrinth = "jei"
        curseforge = "jei"

[modproperties.examplemod]
    p = "q"

[[accessTransformers]]
    file = "META-INF/accesstransformer.cfg"
""".trimIndent()
        val actual = Toml.encodeToString(fullModel())
        assertEquals(expected.trimEnd(), actual.trimEnd())
    }

    @Test
    fun minimalModelOmitsNulls() {
        val model = ModsToml(
            modLoader = null,
            loaderVersion = null,
            license = "MIT",
            issueTrackerURL = null,
            showAsResourcePack = null,
            showAsDataPack = null,
            services = null,
            properties = null,
            mods = listOf(
                Mod(
                    "examplemod",
                    null,
                    "1.0",
                    "Example",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                )
            ),
            features = null,
            mixins = null,
            mcPublish = null,
            dependencies = null,
            modproperties = null,
            accessTransformers = null,
        )
        val actual = Toml.encodeToString(model)
        assertFalse(actual.contains("modLoader"))
        assertFalse(actual.contains("loaderVersion"))
        assertFalse(actual.contains("[[mixins]]"))
        assertFalse(actual.contains("[features"))
        assertFalse(actual.contains("accessTransformers"))
        assertFalse(actual.contains("services"))
        assertTrue(actual.contains("license = \"MIT\""))
        assertTrue(actual.contains("modId = \"examplemod\""))
        assertTrue(actual.indexOf("license = \"MIT\"") < actual.indexOf("[[mods]]"))
    }

    @Test
    fun scalarsPrecedeTables() {
        val lines = Toml.encodeToString(fullModel()).lines()
        val firstTable = lines.indexOfFirst { it.startsWith("[") }
        assertTrue(firstTable >= 0)
        val scalarKeys = listOf(
            "license", "issueTrackerURL", "showAsResourcePack", "showAsDataPack",
            "services", "modLoader", "loaderVersion",
        )
        val misplaced = lines.drop(firstTable).filter { line ->
            scalarKeys.any { key -> line.startsWith("$key ") || line.startsWith("$key=") }
        }
        assertTrue(misplaced.isEmpty(), "scalar keys after first table: $misplaced")
    }
}
