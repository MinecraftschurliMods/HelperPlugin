package at.minecraftschurli.gradle.modhelper.modstoml

import com.akuleshov7.ktoml.annotations.TomlLiteral
import com.akuleshov7.ktoml.annotations.TomlMultiline
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName


@Serializable
data class ModsToml(
    // scalars must stay above `mods`, ktoml emits in declaration order
    val modLoader: String?,
    val loaderVersion: String?,
    val license: String,
    val issueTrackerURL: String?,
    val showAsResourcePack: Boolean?,
    val showAsDataPack: Boolean?,
    val services: List<String>?,
    val properties: Map<String, String>?,
    val mods: List<Mod>,
    val features: Map<String, Map<String, String>>?,
    val mixins: List<Mixin>?,
    @SerialName("mc-publish")
    val mcPublish: McPublish?,
    val dependencies: Map<String, List<Dependency>>?,
    val modproperties: Map<String, Map<String, String>>?,
    val accessTransformers: List<AccessTransformer>?,
) : java.io.Serializable

@Serializable
data class Mod(
    val modId: String,
    val namespace: String?,
    val version: String,
    val displayName: String,
    val displayURL: String?,
    val updateJSONURL: String?,
    val modUrl: String?,
    val logoFile: String?,
    val logoBlur: Boolean?,
    val credits: String?,
    val authors: String?,
    @TomlLiteral
    @TomlMultiline
    val description: String?,
    val featureFlags: String?,
    val enumExtensions: String?,
) : java.io.Serializable

@Serializable
data class AccessTransformer(
    val file: String
) : java.io.Serializable

@Serializable
data class Mixin(
    val config: String,
    val requiredMods: List<String>?,
    val behaviorVersion: String?,
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
    val reason: String?,
    val ordering: String?,
    val side: String?,
    val referralUrl: String?,
    @SerialName("mc-publish")
    val mcPublish: DependencyMcPublish?,
) : java.io.Serializable

@Serializable
data class DependencyMcPublish(
    val modrinth: String?,
    val curseforge: String?,
) : java.io.Serializable
