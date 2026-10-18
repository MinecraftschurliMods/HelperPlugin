package at.minecraftschurli.gradle.modhelper.moddependencies

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.provider.Provider

open class ModDependencyContainer(
    private val project: Project,
    private val delegate: NamedDomainObjectContainer<ModDependency>
) : NamedDomainObjectContainer<ModDependency> by delegate {
    override fun create(name: String) = delegate.create(name) {
        // created eagerly so the repository exists before NeoGradle resolves anything, see addModDependencyRepository.
        // the repository is added here and not in configureEach, as create adds the object before running the action
        project.addModDependencyRepository(this)
    }
    override fun create(name: String, configurationAction: Action<in ModDependency>) = delegate.create(name) {
        configurationAction.execute(this)
        // created eagerly so the repository exists before NeoGradle resolves anything, see addModDependencyRepository.
        // the repository is added here and not in configureEach, as create adds the object before running the action
        project.addModDependencyRepository(this)
    }
    override fun register(name: String) = throw UnsupportedOperationException("ModDependencyContainer does not support lazy registration, use create instead")
    override fun register(name: String, configurationAction: Action<in ModDependency>) = throw UnsupportedOperationException("ModDependencyContainer does not support lazy registration, use create instead")

    fun optional(name: String, cfg: Action<ModDependency> = Action {}) = create(name) {
        type.set(ModDependency.Type.OPTIONAL)
        cfg.execute(this)
    }
    fun required(name: String, cfg: Action<ModDependency> = Action {}) = create(name) {
        type.set(ModDependency.Type.REQUIRED)
        cfg.execute(this)
    }
    fun incompatible(name: String, cfg: Action<ModDependency> = Action {}) = create(name) {
        type.set(ModDependency.Type.INCOMPATIBLE)
        cfg.execute(this)
    }
    fun discouraged(name: String, cfg: Action<ModDependency> = Action {}) = create(name) {
        type.set(ModDependency.Type.DISCOURAGED)
        cfg.execute(this)
    }

    fun jei(cfg: Action<ModDependency> = Action {}) = optional("jei") {
        artifact("mezz.jei:jei-{mc}-neoforge:{version}")
        apiArtifact("mezz.jei:jei-{mc}-common-api:{version}")
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
    fun geckolib(cfg: Action<ModDependency> = Action {}) = required("geckolib") {
        ordering.set(ModDependency.Ordering.BEFORE)
        side.set(ModDependency.Side.BOTH)
        artifact("com.geckolib:geckolib-neoforge-{mc}:{version}")
        repository("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/")
        cfg.execute(this)
    }

    operator fun invoke(cfg: Action<ModDependencyContainer>): ModDependencyContainer {
        cfg.execute(this)
        return this
    }

    companion object {
        fun create(project: Project, minecraftVersion: Provider<String>, runningInCI: Provider<Boolean>): ModDependencyContainer = ModDependencyContainer(project, project.objects.domainObjectContainer(ModDependency::class.java) { name ->
            ModDependency(
                name,
                project,
                minecraftVersion,
                runningInCI
            )
        })
    }
}
