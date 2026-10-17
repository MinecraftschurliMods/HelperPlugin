package com.github.minecraftschurlimods.helperplugin.modstoml

import com.akuleshov7.ktoml.Toml
import kotlinx.serialization.encodeToString
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Writes a small file, not worth caching")
open class GenerateModsTomlTask : DefaultTask() {
    @Input
    val modsToml: Property<ModsToml> = project.objects.property()
    @OutputFile
    val modsTomlFile: RegularFileProperty = project.objects.fileProperty()
        .convention(project.layout.buildDirectory.file("generated/modsToml/mods.toml"))

    @TaskAction
    fun writeModsToml() {
        val toml = modsToml.get()
        toml.mods.forEach { mod ->
            require(mod.description?.contains("'''") != true) { "Description of mod ${mod.modId} must not contain ''' as it is written as a TOML multiline literal string" }
        }
        modsTomlFile.asFile.get().writeText(Toml.encodeToString(toml) + "\n")
    }
}
