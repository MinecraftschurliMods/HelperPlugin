package com.github.minecraftschurlimods.helperplugin

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.the

// TODO: remove this once https://github.com/gradle/gradle/issues/23572 is fixed
fun Project.localGradleProperty(name: Provider<String>): Provider<String> = name.map {
    if (hasProperty(it))
        property(it)?.toString()!!
    else
        null!!
}

fun Project.localGradleProperty(name: String): Provider<String> = localGradleProperty(provider { name })



