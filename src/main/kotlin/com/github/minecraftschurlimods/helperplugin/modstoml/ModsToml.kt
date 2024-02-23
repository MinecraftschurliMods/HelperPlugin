package com.github.minecraftschurlimods.helperplugin.modstoml

import com.akuleshov7.ktoml.annotations.TomlLiteral
import com.akuleshov7.ktoml.annotations.TomlMultiline
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName


@Serializable
data class ModsToml(
    val modLoader: String,
    val loaderVersion: String,
    val license: String,
    val issueTrackerURL: String?,
    val mods: List<Mod>,
    @SerialName("mc-publish")
    val mcPublish: McPublish?,
    val dependencies: Map<String, List<Dependency>>?,
    val modproperties: Map<String, Map<String, String>>?,
) : java.io.Serializable

@Serializable
data class Mod(
    val modId: String,
    val version: String,
    val displayName: String,
    val displayURL: String?,
    val logoFile: String?,
    val credits: String?,
    val authors: String?,
    @TomlLiteral
    @TomlMultiline
    val description: String?,
) : java.io.Serializable

@Serializable
data class McPublish(
    val modrinth: String?,
    val curseforge: Int?,
) : java.io.Serializable

@Serializable
data class Dependency(
    val modId: String,
    val versionRange: String,
    val type: String,
    val ordering: String?,
    val side: String?,
) : java.io.Serializable
