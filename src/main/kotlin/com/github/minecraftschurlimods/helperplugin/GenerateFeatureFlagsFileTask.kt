package com.github.minecraftschurlimods.helperplugin

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

open class GenerateFeatureFlagsFileTask : DefaultTask() {
    @Input
    val featureFlags: ListProperty<String> = project.objects.listProperty(String::class.java)
    @OutputFile
    val featureFlagsFile: RegularFileProperty = project.objects.fileProperty()
        .convention(project.layout.buildDirectory.file("generated/featureFlags/feature_flags.json"))

    @TaskAction
    fun writeFeatureFlagsFile() {
        val flags = featureFlags.orNull ?: listOf()
        if (flags.isEmpty()) {
            return
        }
        featureFlagsFile.asFile.get().writeText("{\"flags\":[${flags.joinToString(",") { "\"$it\""}}]}")
    }
}
