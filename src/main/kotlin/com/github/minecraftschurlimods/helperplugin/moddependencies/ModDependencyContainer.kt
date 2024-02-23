package com.github.minecraftschurlimods.helperplugin.moddependencies

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import javax.inject.Inject

open class ModDependencyContainer @Inject constructor(project: Project) : NamedDomainObjectContainer<ModDependency> by project.objects.domainObjectContainer(ModDependency::class.java, { name -> ModDependency(name, project) }) {
    fun optional(name: String, cfg: Action<ModDependency> = Action {}) = register(name) {
        type.set(ModDependency.Type.OPTIONAL)
        cfg.execute(this)
    }
    fun required(name: String, cfg: Action<ModDependency> = Action {}) = register(name) {
        type.set(ModDependency.Type.REQUIRED)
        cfg.execute(this)
    }

    fun jei(cfg: Action<ModDependency> = Action {}) = optional("jei", cfg)
    fun jade(cfg: Action<ModDependency> = Action {}) = optional("jade", cfg)
    fun theoneprobe(cfg: Action<ModDependency> = Action {}) = optional("theoneprobe") {
        curseforgeId.set("the-one-probe")
        modrinthId.set("the-one-probe")
        cfg.execute(this)
    }
    fun curios(cfg: Action<ModDependency> = Action {}) = optional("curios") {
        side.set(ModDependency.Side.BOTH)
        cfg.execute(this)
    }
    fun configured(cfg: Action<ModDependency> = Action {}) = optional("configured", cfg)
    fun catalogue(cfg: Action<ModDependency> = Action {}) = optional("catalogue", cfg)

    operator fun invoke(cfg: Action<ModDependencyContainer>): ModDependencyContainer {
        cfg.execute(this)
        return this
    }
}
