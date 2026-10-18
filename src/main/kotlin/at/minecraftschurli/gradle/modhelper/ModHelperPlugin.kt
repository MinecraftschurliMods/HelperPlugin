package at.minecraftschurli.gradle.modhelper

import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("unused")
class ModHelperPlugin : Plugin<Any> {
    override fun apply(target: Any) {
        if (target is Project) {
            target.plugins.apply(ModHelperProjectPlugin::class.java)
        }
    }
}