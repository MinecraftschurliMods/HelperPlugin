package com.github.minecraftschurlimods.helperplugin

import com.github.minecraftschurlimods.helperplugin.moddependencies.ModDependency
import net.neoforged.gradle.common.extensions.JarJarExtension
import net.neoforged.gradle.dsl.common.runs.ide.extensions.IdeaRunExtension
import net.neoforged.gradle.dsl.common.runs.run.Run
import net.neoforged.gradle.util.TransformerUtils
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.the
import org.gradle.language.jvm.tasks.ProcessResources

// TODO: remove this once https://github.com/gradle/gradle/issues/23572 is fixed
fun Project.localGradleProperty(name: Provider<String>): Provider<String> = name.map(TransformerUtils.guard {
    return@guard if (hasProperty(it)) property(it)?.toString() else null
})

fun Project.localGradleProperty(name: String): Provider<String> = localGradleProperty(provider { name })

val Project.base: BasePluginExtension get() = this.the<BasePluginExtension>()
val Project.java: JavaPluginExtension get() = this.the<JavaPluginExtension>()
val Project.sourceSets: SourceSetContainer get() = this.the<SourceSetContainer>()
val Project.publishing: PublishingExtension get() = this.the<PublishingExtension>()
val Project.runs: NamedDomainObjectContainer<Run> get() = this.extensions.getByName("runs") as NamedDomainObjectContainer<Run>
val Project.jarJar: JarJarExtension get() = this.the<JarJarExtension>()

val SourceSetContainer.api: NamedDomainObjectProvider<SourceSet> get() = named("api")
val SourceSetContainer.main: NamedDomainObjectProvider<SourceSet> get() = named("main")
val SourceSetContainer.test: NamedDomainObjectProvider<SourceSet> get() = named("test")

val NamedDomainObjectProvider<SourceSet>.output: Provider<SourceSetOutput> get() = map { it.output }
val NamedDomainObjectProvider<SourceSet>.allJava: Provider<SourceDirectorySet> get() = map { it.allJava }
val NamedDomainObjectProvider<SourceSet>.allSource: Provider<SourceDirectorySet> get() = map { it.allSource }

val TaskContainer.processResources: TaskProvider<ProcessResources> get() = named<ProcessResources>("processResources")
val TaskContainer.javadoc: TaskProvider<Javadoc> get() = named<Javadoc>("javadoc")
val TaskContainer.jar: TaskProvider<Jar> get() = named<Jar>("jar")
val TaskContainer.apiJar: TaskProvider<Jar> get() = named<Jar>("apiJar")
val TaskContainer.sourcesJar: TaskProvider<Jar> get() = named<Jar>("sourcesJar")
val TaskContainer.javadocJar: TaskProvider<Jar> get() = named<Jar>("javadocJar")

operator fun JavaPluginExtension.invoke(action: Action<JavaPluginExtension>) = action.execute(this)
operator fun PublishingExtension.invoke(action: Action<PublishingExtension>) = action.execute(this)

val NamedDomainObjectProvider<ModDependency>.version: Provider<String> get() = flatMap { it.version }
val Run.idea:IdeaRunExtension get() = the<IdeaRunExtension>()
