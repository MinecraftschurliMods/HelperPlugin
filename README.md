# HelperPlugin

A Gradle plugin that takes the boilerplate out of NeoForge mod workspaces built with
[NeoGradle](https://github.com/neoforged/NeoGradle). It applies NeoGradle's userdev plugin and
configures most of a mod workspace from `gradle.properties`: publishing, the generated
`neoforge.mods.toml`, source sets, runs, tests and mod dependencies.

## Setup

`settings.gradle.kts`:

```kotlin
pluginManagement {
    plugins {
        id("net.neoforged.gradle.userdev") version "7.1.38"
        id("com.github.minecraftschurlimods.helperplugin") version "2.0"
    }
    repositories {
        gradlePluginPortal()
        maven { url = uri("https://maven.neoforged.net/releases") }
        maven { url = uri("https://maven.minecraftschurli.at/maven-public") }
    }
}
```

The plugin depends on NeoGradle through a version range, so the NeoGradle version is picked by the
mod, not by the plugin release.

`build.gradle.kts`:

```kotlin
plugins {
    id("com.github.minecraftschurlimods.helperplugin")
}

helper.withApiSourceSet()
helper.withDataGenSourceSet()
helper.withTestSourceSet()

helper.withCommonRuns()
helper.withDataGenRuns()
helper.withGameTestRuns()
```

## Configuration through `gradle.properties`

| Property                                                               | Used for                                                                     |
|------------------------------------------------------------------------|------------------------------------------------------------------------------|
| `mod.group`, `mod.id`, `mod.version`, `mod.name`, `mod.vendor`         | Maven coordinates, archive name, mods.toml                                   |
| `mod.description`, `mod.authors`, `mod.credits`, `mod.logo`, `mod.url` | mods.toml                                                                    |
| `mod.update_url`, `mod.mod_url`                                        | mods.toml `updateJSONURL` / `modUrl`                                         |
| `mc_version`, `mc_version_range`                                       | Version suffix, Minecraft dependency, run/toml layout                        |
| `neo_version`, `neo_version_range`                                     | NeoForge dependency                                                          |
| `java_version`                                                         | Java toolchain                                                               |
| `license.name`, `license.url`, `license.file`                          | mods.toml, POM, file bundled in jars                                         |
| `github.owner`, `github.repo`                                          | POM scm/issues/CI urls, mods.toml `issueTrackerURL`                          |
| `loader.name`, `loader.version`                                        | mods.toml `modLoader` / `loaderVersion` (optional)                           |
| `mc-publish.curseforge`, `mc-publish.modrinth`                         | `[mc-publish]` section for publishing actions                                |
| `dependency.<modid>.version`, `dependency.<modid>.version.range`       | Mod dependencies, see below                                                  |
| `junit_version`                                                        | JUnit BOM for the test source set                                            |
| `project_type`                                                         | `MOD` (default) or `LIBRARY` (uses `lib.*` instead of `mod.*`, no mods.toml) |

Every value is also exposed as a property on the `helper` extension and can be set there instead.

The version is `<mc_version>-<mod.version>`, suffixed with the `RELEASE_TYPE` environment variable
(`SNAPSHOT` if unset, nothing for `release`). Publishing goes to `MAVEN_URL` with
`MAVEN_USER`/`MAVEN_PASSWORD`, or to `build/repo` if those aren't set.

## Source sets and runs

| Function                  | Effect                                                                                                                   |
|---------------------------|--------------------------------------------------------------------------------------------------------------------------|
| `withApiSourceSet()`      | `api` source set, published as its own feature variant, included in the main jar, the only source set covered by javadoc |
| `withDataGenSourceSet()`  | `data` source set, used by the data generation runs                                                                      |
| `withTestSourceSet(cfg)`  | `test` source set for JUnit tests, with the NeoForge testframework and a `junit` run, see [Tests](#tests)                |
| `withGameTestSourceSet()` | `gametest` source set for game tests                                                                                     |
| `withCommonRuns()`        | `client` and `server` runs                                                                                               |
| `withDataGenRuns(cfg)`    | `clientData` + `serverData` runs (`data` before 1.21.4), output to `src/main/generated`                                  |
| `withGameTestRuns(cfg)`   | `gameTestServer` run, adds the `gametest` source set to it and to `client`                                               |
| `withJarJar()`            | Makes the jar-in-jar jar the main artifact (the plain jar gets the `slim` classifier)                                    |

Call order doesn't matter.

### Dependency layering

- `test`, `data` and `gametest` inherit main's `implementation`, `runtimeOnly` and `compileOnly`
  dependencies. Nothing flows back into main.
- `api` inherits nothing from main. Only `modCompileOnly` reaches it.
- `modCompileOnly` is for things every source set compiles against, including `api`, such as
  annotations:

```kotlin
dependencies {
    "modCompileOnly"("org.jetbrains:annotations:23.0.0")
}
```

NeoForge is added to main's `implementation` automatically.

## Tests

- **`test`** holds JUnit tests and runs through NeoGradle's `junit` run, so `./gradlew test` runs
  inside the modded environment. Plain tests run as normal; tests that need registry or datapack
  data can ask for an ephemeral server:

  ```java
  @Test
  @ExtendWith(EphemeralTestServerProvider.class)
  void itemsAreRegistered(MinecraftServer server) {
      assertNotNull(server.registryAccess().lookupOrThrow(Registries.ITEM));
  }
  ```

- **`gametest`** holds world-based game tests, with their structures and test data in
  `src/gametest/resources`. Run them with `./gradlew runGameTestServer` or `/test` in the client.

## Mod dependencies

Mod dependencies are declared once and produce both the `[[dependencies.<modid>]]` entries in
mods.toml and, if they declare an artifact, the Gradle dependencies and repository.
`neoforge` and `minecraft` are always declared. They live in the `mods` extension of the
`dependencies` block (`helper.dependencies` is the same container):

```kotlin
dependencies {
    mods.jei()
    mods.curios()
    mods.required("patchouli") {
        ordering = ModDependency.Ordering.BEFORE
        artifact("vazkii.patchouli:patchouli-neoforge:{version}", apiClassifier = "api")
        repository("https://maven.blamejared.com/")
    }
}
```

The version comes from `dependency.<modid>.version`, and the mods.toml range from
`dependency.<modid>.version.range`. In artifact notations `{mc}` and `{version}` are replaced by the
Minecraft and dependency version. `artifact { mc, version -> "..." }` works as well.

`repository(url)` adds a repository restricted to the artifact's group (pass `group = "..."` to
choose another). It is added as soon as the dependency is declared, so it is also available to
configurations NeoGradle resolves while configuring the project, such as `interfaceInjection`.

How artifacts are added (to main unless `usedBy(...)` names other source sets):

|            | Without api artifact        | With api artifact                               |
|------------|-----------------------------|-------------------------------------------------|
| `required` | `implementation`            | api to `compileOnly`, full jar to `runtimeOnly` |
| `optional` | `compileOnly` + run runtime | api to `compileOnly`, full jar to run runtime   |

- **Run runtime** means NeoGradle's `localRunRuntime` configuration: the jar is on the runs'
  classpath but on no compile classpath and not in the published POM. It is skipped in CI; set
  `devRuntime` to override that per dependency.
- **`api` as a target** only ever gets the compile artifact.
- **Presets** `jei()`, `jade()`, `curios()` and `geckolib()` come with coordinates and
  repositories, and everything can be overridden in their configuration block. `theoneprobe()`, `configured()` and
  `catalogue()` only declare the mods.toml entry.

Declaring returns the `ModDependency`, so its `artifact`, `apiArtifact` and `version` providers
can be used for anything unusual:

```kotlin
dependencies {
    val curios = mods.curios()
    val geckolib = mods.geckolib()
    "dataRuntimeOnly"(curios.artifact)
    "interfaceInjection"(geckolib.artifact)
}
```

## mods.toml

`generateModsToml` writes `META-INF/neoforge.mods.toml` (`mods.toml` before 1.20.6) into the jar from
the properties above and these extension properties:

| Property                                           | mods.toml                                                                                                            |
|----------------------------------------------------|----------------------------------------------------------------------------------------------------------------------|
| `accessTransformers`                               | `[[accessTransformers]]`, defaults to `META-INF/accesstransformer.cfg` if that file exists, also passed to NeoGradle |
| `mixin(config) { requiredMods; behaviorVersion }`  | `[[mixins]]`                                                                                                         |
| `featureFlags`                                     | generated `META-INF/feature_flags.json` (flags are prefixed with the mod id)                                         |
| `enumExtensions`                                   | `enumExtensions`                                                                                                     |
| `features`                                         | `[features.<modid>]`, e.g. `javaVersion`                                                                             |
| `modproperties`                                    | `[modproperties.<modid>]`                                                                                            |
| `fileProperties`                                   | `[properties]`                                                                                                       |
| `services`, `showAsResourcePack`, `showAsDataPack` | same name                                                                                                            |
| `projectNamespace`, `projectLogoBlur`              | `namespace`, `logoBlur`                                                                                              |

Entries that are empty or unset are omitted.

## Other conveniences

- **Manifests:** jars get a manifest with the Maven coordinates, Minecraft/NeoForge versions,
  `FMLModType` and license.
- **JSON:** JSON resources are minified when they are packaged.
- **CI:** `setupGithubActions` writes the mod id, version and Minecraft version to `$GITHUB_OUTPUT`
  when it is set.

## Development

```shell
./gradlew build           # compile, plugin validation, unit tests
./gradlew functionalTest  # TestKit tests against real NeoGradle (slow, needs network)
```

To try a change in a mod, run `./gradlew publishToMavenLocal` and add `mavenLocal()` to the mod's
`pluginManagement` repositories.

## License

[MIT](LICENSE)
