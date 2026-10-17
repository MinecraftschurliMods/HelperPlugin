@file:Suppress("UnstableApiUsage")

package com.github.minecraftschurlimods.helperplugin

import net.neoforged.gradle.common.tasks.JarJar
import net.neoforged.gradle.dsl.common.extensions.subsystems.Subsystems
import net.neoforged.gradle.dsl.common.runs.run.Run
import org.gradle.api.Action
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.named

// owns all source set and run wiring for HelperExtension
internal class RunConfigurator(private val project: Project, private val ext: HelperExtension) {
    // all source sets derived from main (test, gametest, data), so that the api source set can be added to them
    private val derivedSourceSets: DomainObjectSet<SourceSet> = project.objects.domainObjectSet(SourceSet::class.java)
    private var apiSourceSet: SourceSet? = null
    private var testSourceSet: SourceSet? = null
    private var gameTestSourceSet: SourceSet? = null
    private var dataGenSourceSet: SourceSet? = null

    private fun createDerivedSourceSet(name: String): SourceSet {
        val sourceSet = project.sourceSets.maybeCreate(name)
        val main = project.sourceSets.main.get()
        // derived source sets see everything main sees (never the other way around)
        project.configurations.named(sourceSet.implementationConfigurationName) { extendsFrom(project.configurations[main.implementationConfigurationName]) }
        project.configurations.named(sourceSet.runtimeOnlyConfigurationName) { extendsFrom(project.configurations[main.runtimeOnlyConfigurationName]) }
        project.configurations.named(sourceSet.compileOnlyConfigurationName) { extendsFrom(project.configurations[main.compileOnlyConfigurationName]) }
        project.dependencies {
            sourceSet.implementationConfigurationName(project.sourceSets.main.output)
        }
        derivedSourceSets.add(sourceSet)
        return sourceSet
    }

    fun withJarJar() {
        project.tasks.named<Jar>("jar") {
            archiveClassifier.set("slim")
        }
        project.tasks.named<JarJar>("jarJar") {
            archiveClassifier.set("")
        }
    }

    fun withApiSourceSet() {
        if (apiSourceSet != null) return
        val apiSet = project.sourceSets.maybeCreate("api")
        apiSourceSet = apiSet
        project.java.registerFeature("api") {
            usingSourceSet(apiSet)
        }
        // api must not inherit from main, it only gets the shared compile only dependencies
        project.configurations.named(apiSet.compileOnlyConfigurationName) { extendsFrom(project.configurations[MOD_COMPILE_ONLY]) }
        project.dependencies {
            apiSet.compileOnlyConfigurationName(ext.neoforge())
            "implementation"(apiSet.output)
        }
        derivedSourceSets.all {
            project.dependencies.add(implementationConfigurationName, apiSet.output)
        }
        project.tasks.jar {
            from(apiSet.output)
        }
        project.tasks.sourcesJar {
            from(apiSet.allSource)
        }
        project.tasks.apiJar {
            from(apiSet.allSource)
        }
        project.tasks.javadoc {
            classpath = apiSet.compileClasspath
            source = apiSet.allJava
        }
        project.runs.all {
            modSources.add(apiSet)
        }
    }

    fun withTestSourceSet(cfg: Action<Run> = Action {}) {
        if (testSourceSet != null) return
        val testSet = createDerivedSourceSet("test")
        testSourceSet = testSet
        val junitPlatform = project.dependencies.platform("org.junit:junit-bom:${ext.junitVersion.get()}")
        project.dependencies {
            testSet.implementationConfigurationName(ext.testframework())
            testSet.implementationConfigurationName(junitPlatform)
            testSet.implementationConfigurationName("org.junit.jupiter:junit-jupiter")
            testSet.runtimeOnlyConfigurationName("org.junit.platform:junit-platform-launcher")
        }
        // let NeoGradle configure the stock test task instead of creating its own (off by default)
        project.extensions.getByType<Subsystems>().conventions.runs.shouldDefaultTestTaskBeReused.set(true)
        project.runs.maybeCreate("junit").apply {
            isJUnit.set(true)
            runType("junit")
            singleInstance()
            jvmArgument("-ea")
            modSources.add(project.sourceSets.main.get())
            unitTestSources.add(testSet)
            cfg.execute(this)
        }
        // NeoGradle only reuses the stock test task if the junit run's mod sources are exactly {main};
        // otherwise (e.g. with the api source set) it creates testJunit, so forward test to it
        project.tasks.test {
            val junitTask = project.tasks.findByName("testJunit") ?: return@test
            enabled = false
            dependsOn(junitTask)
        }
    }

    fun withGameTestSourceSet() {
        if (gameTestSourceSet != null) return
        gameTestSourceSet = createDerivedSourceSet("gametest")
    }

    fun withDataGenSourceSet() {
        if (dataGenSourceSet != null) return
        val dataSet = createDerivedSourceSet("data")
        dataGenSourceSet = dataSet
        project.runs.matching { it.isDataRun() }.all {
            modSources.add(dataSet)
            idea.primarySourceSet = dataSet
        }
    }

    fun withCommonRuns() {
        project.runs {
            maybeCreate("client")
            maybeCreate("server").apply {
                singleInstance()
                arguments.add("--nogui")
            }
        }
    }

    fun withDataGenRuns(cfg: Action<Run> = Action<Run>{}) {
        if (ext.useSplitDataRuns.get()) {
            project.runs.maybeCreate("clientData")
            project.runs.maybeCreate("serverData")
        } else {
            project.runs.maybeCreate("data")
        }
        project.runs.matching { it.isDataRun() }.all {
            singleInstance()
            arguments.add("--mod")
            arguments.add(ext.projectId)
            arguments.add("--all")
            arguments.add("--output")
            arguments.add(ext.generatedResourcesDir.map { it.asFile.absolutePath })
            arguments.add("--existing")
            arguments.add(project.layout.projectDirectory.dir("src/main/resources/").asFile.absolutePath)
            cfg.execute(this)
        }
        project.sourceSets.main {
            resources {
                srcDir(ext.generatedResourcesDir)
                exclude(".cache")
            }
        }
    }

    fun withGameTestRuns(cfg: Action<Run> = Action<Run>{}) {
        withGameTestSourceSet()
        val gameTest = gameTestSourceSet!!
        project.runs.maybeCreate("gameTestServer").apply {
            singleInstance()
            jvmArgument("-ea")
            modSources.add(gameTest)
            systemProperties.put("neoforge.enabledGameTestNamespaces", ext.projectId)
            idea.primarySourceSet = gameTest
            cfg.execute(this)
        }
        project.runs.matching { it.name == "client" }.configureEach {
            modSources.add(gameTest)
            systemProperties.put("neoforge.enabledGameTestNamespaces", ext.projectId)
        }
    }

    private fun Run.isDataRun(): Boolean = if (ext.useSplitDataRuns.get()) name == "clientData" || name == "serverData" else name == "data"
}
