package at.minecraftschurli.gradle.modhelper

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.file.FileCollection

/**
 * Minifies every json file in the given file collection to reduce jar size.
 */
class MinifyJsonAction(private val files: FileCollection) : Action<Task> {
    override fun execute(task: Task) {
        files.asFileTree.matching { include("**/*.json") }.forEach {
            it.writeText(JsonOutput.toJson(JsonSlurper().parse(it)))
        }
    }
}
