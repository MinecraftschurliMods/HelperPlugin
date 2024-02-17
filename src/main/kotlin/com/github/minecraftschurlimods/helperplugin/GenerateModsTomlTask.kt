package com.github.minecraftschurlimods.helperplugin

import com.akuleshov7.ktoml.Toml
import kotlinx.serialization.encodeToString
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property

open class GenerateModsTomlTask : DefaultTask() {
    @Input
    val modsToml: Property<ModsToml> = project.objects.property()
    @OutputFile
    val modsTomlFile: RegularFileProperty = project.objects.fileProperty()
        .convention(project.layout.buildDirectory.file("generated/modsToml/mods.toml"))

    @TaskAction
    fun writeModsToml() {
        modsTomlFile.asFile.get().writeText(Toml.encodeToString(modsToml.get()) + "\n")
    }
}
