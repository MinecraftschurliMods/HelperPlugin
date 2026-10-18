package at.minecraftschurli.gradle.modhelper.moddependencies

import at.minecraftschurli.gradle.modhelper.localGradleProperty
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.setProperty

class ModDependency(val name: String, project: Project, private val minecraftVersion: Provider<String>, runningInCI: Provider<Boolean>) {

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
    val reason: Property<String> = project.objects.property()
    val referralUrl: Property<String> = project.objects.property()

    // maven coordinates of the full mod jar and of its api jar, without an artifact nothing is added to the classpath
    val artifact: Property<String> = project.objects.property()
    val apiArtifact: Property<String> = project.objects.property()
    // maven repository the artifacts are resolved from, restricted to repositoryGroup (defaults to the artifact's group)
    val repository: Property<String> = project.objects.property()
    val repositoryGroup: Property<String> = project.objects.property()
    // whether optional dependencies are put on the runtime of the runs, off in CI by default
    val devRuntime: Property<Boolean> = project.objects.property<Boolean>()
        .convention(runningInCI.map { !it }.orElse(true))
    // source sets the artifacts are added to, defaults to main (which the derived source sets inherit from)
    val usedBy: SetProperty<String> = project.objects.setProperty<String>()

    /**
     * Sets the artifact from a notation where `{mc}` and `{version}` are replaced by the minecraft and dependency version.
     * If [apiClassifier] is given, the api artifact is the same artifact with that classifier.
     */
    fun artifact(notation: String, apiClassifier: String? = null) = artifact(resolve(notation), apiClassifier)

    fun artifact(notation: (mc: String, version: String) -> String, apiClassifier: String? = null) = artifact(minecraftVersion.zip(version, notation), apiClassifier)

    fun artifact(notation: (version: String) -> String, apiClassifier: String? = null) = artifact(version.map(notation), apiClassifier)

    fun artifact(notation: (mc: String, version: String) -> String) = artifact(minecraftVersion.zip(version, notation))

    fun artifact(notation: (version: String) -> String) = artifact(version.map(notation))

    fun artifact(notation: Provider<String>, apiClassifier: String? = null) {
        artifact.set(notation)
        if (apiClassifier != null) {
            apiArtifact(artifact.map { "$it:$apiClassifier" })
        }
    }

    fun apiArtifact(notation: String) = apiArtifact(resolve(notation))

    fun apiArtifact(notation: (mc: String, version: String) -> String) = apiArtifact(minecraftVersion.zip(version, notation))

    fun apiArtifact(notation: (version: String) -> String) = apiArtifact(version.map(notation))

    fun apiArtifact(notation: Provider<String>) = apiArtifact.set(notation)

    /**
     * Sets the maven repository the artifacts are resolved from, restricted to [group] (defaults to the artifact's group).
     */
    fun repository(url: String, group: String? = null) {
        repository.set(url)
        repositoryGroup.set(group)
    }

    fun usedBy(vararg sourceSets: String) = usedBy.addAll(*sourceSets)

    private fun resolve(notation: String) = minecraftVersion.zip(version) { mc, version -> notation.replace("{mc}", mc).replace("{version}", version) }

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
