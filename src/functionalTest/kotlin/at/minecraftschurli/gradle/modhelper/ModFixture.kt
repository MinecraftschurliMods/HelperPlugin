package at.minecraftschurli.gradle.modhelper

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File
import kotlin.collections.iterator

/**
 * A minimal mod project using the plugin from the plugin under test classpath.
 *
 * The fixtures avoid resolving configurations, as that would trigger NeoGradle's full minecraft setup.
 * Instead [report] prints values computed after the project is evaluated.
 */
class ModFixture(val dir: File) {
    init {
        file("settings.gradle.kts").writeText("rootProject.name = \"examplemod\"\n")
        file("gradle.properties").writeText(
            """
            org.gradle.jvmargs=-Xmx3G
            java_version=21
            mod.group=com.example
            mod.id=examplemod
            mod.version=1.0.0
            mod.name=Example Mod
            mod.description=An example mod
            mc_version=26.1.2
            mc_version_range=[26.1,26.2)
            neo_version=26.1.2.99
            neo_version_range=[26.1.2.99,26.2)
            license.name=MIT
            github.owner=Example
            github.repo=example
            dependency.jei.version=29.37.0.99
            dependency.jei.version.range=[29.37,30)
            dependency.geckolib.version=5.5.2
            dependency.geckolib.version.range=[5.5.2,6)
            dependency.patchouli.version=26.1-94
            dependency.patchouli.version.range=[26.1-94,26.2)

            """.trimIndent()
        )
    }

    fun file(path: String): File = File(dir, path).also { it.parentFile.mkdirs() }

    fun buildScript(body: String) = file("build.gradle.kts").writeText(
        "plugins {\n    id(\"at.minecraftschurli.gradle.modhelper\")\n}\n\n" + body.trimIndent() + "\n"
    )

    /**
     * Publishes a library with only an interface injection variant (as NeoGradle's interfaceInjection configuration
     * requests) to a maven repository in the project dir, returns the repository's url.
     */
    fun interfaceInjectionRepository(group: String, module: String, version: String): String {
        val repo = file("local-repo")
        val dir = File(repo, "${group.replace('.', '/')}/$module/$version").also { it.mkdirs() }
        val base = "$module-$version"
        val json = File(dir, "$base-interface-injections.json").apply { writeText("{}") }
        File(dir, "$base.pom").writeText(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
              <!-- do_not_remove: published-with-gradle-metadata -->
              <modelVersion>4.0.0</modelVersion>
              <groupId>$group</groupId>
              <artifactId>$module</artifactId>
              <version>$version</version>
            </project>
            """.trimIndent()
        )
        File(dir, "$base.module").writeText(
            """
            {
              "formatVersion": "1.1",
              "component": { "group": "$group", "module": "$module", "version": "$version" },
              "variants": [
                {
                  "name": "interfaceInjectionElements",
                  "attributes": { "org.gradle.category": "interfaceinjection" },
                  "files": [ { "name": "${json.name}", "url": "${json.name}", "size": ${json.length()} } ]
                }
              ]
            }
            """.trimIndent()
        )
        return repo.toURI().toString()
    }

    fun run(vararg arguments: String): BuildResult = GradleRunner.create()
        .withProjectDir(dir)
        .withPluginClasspath()
        .withArguments(*arguments, "--stacktrace")
        .forwardOutput()
        .build()

    companion object {
        /**
         * Build script snippet printing `REPORT <key>=<value>` lines in afterEvaluate (registered after, so run after, the
         * plugin's and NeoGradle's afterEvaluate hooks), run it with any task, e.g. help.
         * [entries] are kotlin expressions evaluated in the build script, keyed by name.
         */
        fun report(entries: Map<String, String>): String = buildString {
            appendLine("afterEvaluate {")
            for ((key, expression) in entries) {
                appendLine("    println(\"REPORT $key=\" + ($expression))")
            }
            appendLine("}")
        }

        // kotlin expression listing the declared (and inherited) external dependencies of a configuration
        fun declaredDependencies(configuration: String) =
            "configurations.getByName(\"$configuration\").allDependencies" +
                ".filterIsInstance<org.gradle.api.artifacts.ExternalModuleDependency>()" +
                ".map { listOfNotNull(it.group, it.name, it.version, it.artifacts.firstOrNull()?.classifier).joinToString(\":\") }" +
                ".sorted()"
    }
}

fun BuildResult.report(key: String): String =
    output.lineSequence().firstOrNull { it.startsWith("REPORT $key=") }?.substringAfter("=")
        ?: error("No report for $key in output")
