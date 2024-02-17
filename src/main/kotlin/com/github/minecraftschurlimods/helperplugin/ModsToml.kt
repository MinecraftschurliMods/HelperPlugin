package com.github.minecraftschurlimods.helperplugin

import com.akuleshov7.ktoml.annotations.TomlMultiline
import kotlinx.serialization.SerialName


@Serializable
data class ModsToml(
    val modLoader: String,
    val loaderVersion: String,
    val license: String,
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
    val displayURL: String,
    val authors: String,
    @TomlMultiline
    val description: String,
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
    val ordering: String = "NONE",
    val side: String = "BOTH",
) : java.io.Serializable
