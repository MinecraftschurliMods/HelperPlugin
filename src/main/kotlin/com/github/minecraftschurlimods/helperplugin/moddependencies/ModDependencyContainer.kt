package com.github.minecraftschurlimods.helperplugin.moddependencies

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.provider.Provider

open class ModDependencyContainer(private val project: Project, minecraftVersion: Provider<String>, runningInCI: Provider<Boolean>) : NamedDomainObjectContainer<ModDependency> by project.objects.domainObjectContainer(ModDependency::class.java, { name -> ModDependency(name, project, minecraftVersion, runningInCI) }) {
    // created eagerly so the repository exists before NeoGradle resolves anything, see addModDependencyRepository
    fun optional(name: String, cfg: Action<ModDependency> = Action {}) = declare(name, ModDependency.Type.OPTIONAL, cfg)
    fun required(name: String, cfg: Action<ModDependency> = Action {}) = declare(name, ModDependency.Type.REQUIRED, cfg)

    private fun declare(name: String, type: ModDependency.Type, cfg: Action<ModDependency>): NamedDomainObjectProvider<ModDependency> {
        create(name) {
            this.type.set(type)
            cfg.execute(this)
            project.addModDependencyRepository(this)
        }
        return named(name)
    }

    fun jei(cfg: Action<ModDependency> = Action {}) = optional("jei") {
        artifact("mezz.jei:jei-{mc}-neoforge:{version}")
        apiArtifact("mezz.jei:jei-{mc}-neoforge-api:{version}")
        repository("https://maven.blamejared.com/")
        cfg.execute(this)
    }
    fun jade(cfg: Action<ModDependency> = Action {}) = optional("jade") {
        artifact("maven.modrinth:jade:{version}-neoforge")
        repository("https://api.modrinth.com/maven")
        cfg.execute(this)
    }
    fun theoneprobe(cfg: Action<ModDependency> = Action {}) = optional("theoneprobe") {
        curseforgeId.set("the-one-probe")
        modrinthId.set("the-one-probe")
        cfg.execute(this)
    }
    fun curios(cfg: Action<ModDependency> = Action {}) = optional("curios") {
        side.set(ModDependency.Side.BOTH)
        artifact("top.theillusivec4.curios:curios-neoforge:{version}", apiClassifier = "api")
        repository("https://maven.theillusivec4.top/")
        cfg.execute(this)
    }
    fun configured(cfg: Action<ModDependency> = Action {}) = optional("configured", cfg)
    fun catalogue(cfg: Action<ModDependency> = Action {}) = optional("catalogue", cfg)

    operator fun invoke(cfg: Action<ModDependencyContainer>): ModDependencyContainer {
        cfg.execute(this)
        return this
    }
}
