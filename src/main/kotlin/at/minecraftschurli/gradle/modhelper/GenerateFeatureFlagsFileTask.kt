package at.minecraftschurli.gradle.modhelper

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Writes a small file, not worth caching")
open class GenerateFeatureFlagsFileTask : DefaultTask() {
    @Input
    val featureFlags: ListProperty<String> = project.objects.listProperty(String::class.java)
    @OutputFile
    val featureFlagsFile: RegularFileProperty = project.objects.fileProperty()
        .convention(project.layout.buildDirectory.file("generated/featureFlags/feature_flags.json"))

    @TaskAction
    fun writeFeatureFlagsFile() {
        val flags = featureFlags.orNull ?: listOf()
        featureFlagsFile.asFile.get().writeText("{\"flags\":[${flags.joinToString(",") { "\"${it.replace("\\", "\\\\").replace("\"", "\\\"")}\"" }}]}")
    }
}
