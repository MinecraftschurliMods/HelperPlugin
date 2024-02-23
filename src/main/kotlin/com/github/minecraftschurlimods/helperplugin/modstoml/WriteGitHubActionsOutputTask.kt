package com.github.minecraftschurlimods.helperplugin.modstoml

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class WriteGitHubActionsOutputTask : DefaultTask() {
    @get:Input
    abstract val values: MapProperty<String, Any>
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun writeOutput() {
        outputFile.get().asFile.appendText(values.get().map { (k, v) -> "$k=$v" }.joinToString("\n"))
    }
}
