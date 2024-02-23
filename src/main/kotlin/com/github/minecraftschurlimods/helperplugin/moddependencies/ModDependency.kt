package com.github.minecraftschurlimods.helperplugin.moddependencies

import com.github.minecraftschurlimods.helperplugin.localGradleProperty
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property

class ModDependency(val name: String, project: Project) {

    val modId: Property<String> = project.objects.property<String>()
        .convention(name)
    val version: Property<String> = project.objects.property<String>()
        .convention(project.localGradleProperty(modId.map { "dependency.${it}.version" }))
    val versionRange: Property<String> = project.objects.property<String>()
        .convention(project.localGradleProperty(modId.map { "dependency.${it}.version.range" }))
    val type: Property<Type> = project.objects.property()
    val ordering: Property<Ordering> = project.objects.property()
    val side: Property<Side> = project.objects.property()
    val modrinthId: Property<String> = project.objects.property()
    val curseforgeId: Property<String> = project.objects.property()

    enum class Type {
        REQUIRED, OPTIONAL, INCOMPATIBLE, DISCOURAGED
    }

    enum class Ordering {
        BEFORE, AFTER, NONE
    }

    enum class Side {
        CLIENT, SERVER, BOTH
    }
}
