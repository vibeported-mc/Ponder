import net.createmod.pondergradle.nullability.PackageInfosExtension

// convention plugin to apply to platform subprojects.

plugins {
    `java-library`
    `maven-publish`
}

// this has to be separate for some reason
plugins.apply("net.createmod.ponder.gradle")
plugins.apply("setup-git-hash")

// set up name and group based on parent project, ex. net.createmod.ponder:ponder-fabric
val modName: String = parent!!.name
base.archivesName = "$modName-$name"
group = "net.createmod.$modName"

// keep version synchronized with the root project
version = rootProject.version

repositories {
    mavenLocal() // TODO: remove when Flywheel is pushed
    maven("https://maven.createmod.net") // Flywheel
    maven("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/") // Forge Config API Port
}

java {
    withSourcesJar()
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks.withType<Jar> {
    // copy the license file into every built jar
    from(rootProject.file("LICENSE"))
}

val libs: VersionCatalog = versionCatalogs.named("libs")
fun versionOf(name: String): String {
    val version = libs.findVersion(name).get().toString()

    // thank you fabric loader for mangling all non-release versions
    // FIXME remove when on full 26.1
    if (name == "minecraft" && project.name == "fabric")
        return version
            .replace("snapshot-", "alpha.")
            .replace("pre-", "pre.")
            .replace("rc-", "rc.")

    return version
}

val authors = findProperty("authors") as String
val contributors = findProperty("contributors") as String

// expand placeholders in metadata files
tasks.processResources {
    val properties = mapOf(
        "version" to project.version,
        "group" to project.group,
        "minecraft_version" to versionOf("minecraft"),
        "neo_version" to versionOf("neoforge"),
        "fabric_api_version" to versionOf("fabric-api"),
        "fabric_loader_version" to versionOf("fabric-loader"),
        "authors" to authors,
        "contributors" to contributors,
        "authors_json" to formatForJson(authors),
        "contributors_json" to formatForJson(contributors)
    )

    inputs.properties(properties)

    filesMatching(setOf("fabric.mod.json", "META-INF/neoforge.mods.toml")) {
        expand(properties)
    }
}

// don't publish the testmod
if (parent!!.name != "testmod") {
    publishing {
        publications.create<MavenPublication>("mavenJava") {
            // Without this the coordinate would be net.createmod.ponder:neoforge, since Gradle
            // defaults the artifact id to the project name rather than the archives name.
            artifactId = base.archivesName.get()
            from(components["java"])
        }

        repositories {
            maven("https://maven.createmod.net") {
                name = "create"
                credentials(PasswordCredentials::class)
            }
        }
    }
}

// will only exist in common/fabric
val loom: Any? = extensions.findByName("loom")
// reflection in the buildscript. have I hit a new low?
// we need to call this now or else the sourceSet won't exist, and
// I don't even know where to begin with compiling against loom here.
loom?.javaClass?.getMethod("splitEnvironmentSourceSets")?.run {
    invoke(loom)
    plugins.apply("register-client-jar")
}

// ModDevGradle 2.0.144 does not know Minecraft 26.2 yet, so its version capability table falls back
// to Java 21 for the NeoFormRuntime tasks. There is no Java 21 on every machine, and Gradle 9 cannot
// auto-provision one (the foojay resolver still references JvmVendorSpec.IBM_SEMERU, which Gradle 9
// removed). The tooling runs fine on the toolchain we already build with.
// NeoFormRuntimeTask lives on the platform project's plugin classpath rather than this one's, so it
// is matched by name and its property set reflectively.
val nfrtJavaExecutable = javaToolchains.launcherFor(java.toolchain)
    .map { it.executablePath.asFile.absolutePath }
tasks.matching { it.javaClass.name.startsWith("net.neoforged.nfrtgradle.") }.configureEach {
    @Suppress("UNCHECKED_CAST")
    val javaExecutable = javaClass.getMethod("getJavaExecutable").invoke(this) as Property<String>
    javaExecutable.set(nfrtJavaExecutable)
}

// generate package-infos for the main (and client, if present) sourceSet(s)
extensions.getByType<PackageInfosExtension>().sources(sourceSets.named { it == "main" || it == "client" })

if (name != "common") {
    // The sibling common project, not the root one: catnip has its own.
    val siblingCommon = parent!!.path + ":common"
    // A platform project pulls the sibling common project's source and class output straight in as
    // extra source/class directories rather than as a project dependency, so Gradle cannot infer the
    // ordering. Every task that reads those directories has to say so explicitly. The package-info
    // generator registers one task per source set, hence the pattern rather than a single name.
    val commonOutputs = project(siblingCommon).tasks.matching {
        it.name.matches(Regex("generate[A-Za-z]*PackageInfos"))
            || it.name in setOf("compileJava", "compileClientJava", "processResources", "processClientResources")
    }
    tasks.withType<Jar> {
        dependsOn(commonOutputs)
    }
    tasks.withType<JavaCompile> {
        dependsOn(commonOutputs)
    }
}

// The config *UI* is not ported yet: config still works, it just has no in-game screens. The
// definitions in api/config (ConfigBase and friends) are what back the TOML files, so they are
// built - Create declares all of its config through them.
// TODO: port the config screens and drop these exclusions.
tasks.withType<JavaCompile> {
    exclude("**/client/config/**")
    // ConfigHelper needs FML's ModConfig/ModConfigs, which Forge Config API Port does not provide,
    // so it cannot live in the common module. Only the config UI, the config command and the config
    // sync packet use it, and all of those are out with the UI.
    exclude("**/config/ConfigHelper.java")
    exclude("**/network/ServerboundConfigPacket.java")
    exclude("**/ConfigCommand.java")
    exclude("**/ConfigPathArgument.java")
    exclude("**/CClient.java")
    exclude("**/PonderConfig.java")
    exclude("**/ConfirmationScreen.java")
}

when (name) {
    "common" -> plugins.apply("provide-common")
    "fabric" -> plugins.apply("consume-common-split")
    else -> plugins.apply("consume-common-merged")
}

// trick to sneak multiple entries into a single placeholder in a JSON file.
// the file must be valid even with placeholders, so we can't just do something like this: [${placeholder}]
// instead, the placeholder is expected to be in a string, like this: ["${placeholder}"]
// this takes a string in the format 'a, b, c' and adds quotes, so the end result will be like this: a", "b", "c
// when filled into the placeholder, you get a valid list: ["a", "b", "c"]
fun formatForJson(entries: String): String {
    return entries.split(", ").joinToString(separator = "\", \"")
}
