package com.github.minecraftschurlimods.helperplugin.modstoml

import com.github.minecraftschurlimods.helperplugin.HelperExtension
import org.gradle.api.Project
import org.gradle.api.provider.Provider

internal fun HelperExtension.toModsToml(project: Project): Provider<ModsToml> = project.provider {
    val modId = projectId.get()

    val dependencyEntries: List<Dependency> = dependencies.map { dep ->
        val depMcPublish = DependencyMcPublish(
            dep.modrinthId.orNull,
            dep.curseforgeId.orNull
        )
        Dependency(
            modId = dep.modId.get(),
            type = dep.type.get().name.lowercase(),
            reason = dep.reason.orNull,
            versionRange = dep.versionRange.get(),
            ordering = dep.ordering.orNull?.name,
            side = dep.side.orNull?.name,
            referralUrl = dep.referralUrl.orNull,
            mcPublish = if (depMcPublish.modrinth != null || depMcPublish.curseforge != null) depMcPublish else null,
        )
    }

    val modsTomlMcPublish = McPublish(
        mcPublish.modrinth.orNull,
        mcPublish.curseforge.orNull
    )

    val mixinEntries: List<Mixin>? = mixins.map {
        Mixin(
            it.config.get(),
            it.requiredMods.orNull?.takeIf { s -> s.isNotEmpty() }?.toList(),
            it.behaviorVersion.orNull,
        )
    }.takeIf { it.isNotEmpty() }

    val modLoader = loader.name.orNull

    ModsToml(
        modLoader = modLoader,
        loaderVersion = if (modLoader != null) loader.version.orNull else null,
        license = license.name.get(),
        issueTrackerURL = gitHub.issuesUrl.orNull,
        showAsResourcePack = showAsResourcePack.orNull,
        showAsDataPack = showAsDataPack.orNull,
        services = services.orNull?.takeIf { it.isNotEmpty() }?.toList(),
        properties = fileProperties.orNull?.takeIf { it.isNotEmpty() },
        mods = listOf(
            Mod(
                modId = modId,
                namespace = projectNamespace.orNull,
                version = projectVersion.get(),
                displayName = projectName.get(),
                displayURL = projectUrl.orNull,
                updateJSONURL = projectUpdateJsonUrl.orNull,
                modUrl = projectModUrl.orNull,
                logoFile = projectLogo.orNull,
                logoBlur = projectLogoBlur.orNull,
                credits = projectCredits.orNull,
                authors = projectAuthors.orNull,
                description = projectDescription.orNull,
                featureFlags = if (featureFlags.getOrElse(emptyList()).isNotEmpty()) "META-INF/feature_flags.json" else null,
                enumExtensions = enumExtensions.orNull,
            )
        ),
        features = features.orNull?.takeIf { it.isNotEmpty() }?.let { mapOf(modId to it) },
        mixins = mixinEntries,
        mcPublish = if (modsTomlMcPublish.modrinth != null || modsTomlMcPublish.curseforge != null) modsTomlMcPublish else null,
        dependencies = dependencyEntries.takeIf { it.isNotEmpty() }?.let { mapOf(modId to it) },
        modproperties = modproperties.orNull?.takeIf { it.isNotEmpty() }?.let { mapOf(modId to it) },
        accessTransformers = accessTransformers.orNull?.takeIf { it.isNotEmpty() }?.map { AccessTransformer(it) },
    )
}
