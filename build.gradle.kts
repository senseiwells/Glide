plugins {
    val jvmVersion = libs.versions.fabric.kotlin.get()
        .split("+kotlin.")[1]
        .split("+")[0]

    kotlin("jvm").version(jvmVersion)
    kotlin("plugin.serialization").version(jvmVersion)
    alias(libs.plugins.fabric.loom)
    `maven-publish`
    java
}

val modVersion = "1.0.0"
version = "${modVersion}+${libs.versions.minecraft.get()}"
group = "me.senseiwells"

repositories {
    mavenLocal()
    maven("https://maven.supersanta.me/snapshots")
    maven("https://maven.parchmentmc.org/")
    maven("https://jitpack.io")
    maven("https://ueaj.dev/maven")
    maven("https://maven.nucleoid.xyz")
    maven("https://maven.maxhenkel.de/repository/public")
    maven("https://maven.mcbrawls.net/releases/")
    mavenCentral()
}

val main: SourceSet = sourceSets.main.get()
val bundled: SourceSet = sourceSets.create("bundled") {
    compileClasspath += main.compileClasspath + main.output
    runtimeClasspath += main.runtimeClasspath + main.output
}

@Suppress("AvoidDuplicateDependencies", "RedundantSuppression") // lmao thanks IntelliJ
dependencies {
    minecraft(libs.minecraft)
    implementation(libs.fabric.loader)
    implementation(libs.fabric.api)
    implementation(libs.fabric.kotlin)

    implementation(libs.arcade)
    implementation(libs.arcade.datagen)
    implementation(libs.polymer.core)

    include(libs.arcade)
    include(libs.polymer.core)
}

val expansions = mapOf(
    "version" to modVersion,
    "minecraft_dependency" to libs.versions.minecraft.get(),
    "fabric_loader_dependency" to libs.versions.fabric.loader.get(),
    "fabric_api_dependency" to libs.versions.fabric.api.get(),
    "fabric_kotlin_dependency" to libs.versions.fabric.kotlin.get()
)

tasks.withType<ProcessResources>().configureEach {
    inputs.properties(expansions)
    filesMatching("fabric.mod.json") {
        expand(expansions)
    }
}

val mainResources = main.output.resourcesDir

tasks.jar {
    from(bundled.output)
    from("LICENSE")

    filesMatching("fabric.mod.json") {
        if (file.parentFile == mainResources) {
            exclude()
        }
    }
}

val devJar = tasks.register<Jar>("devJar") {
    description = "The glide dev jar"
    archiveClassifier = "dev"
    from(main.output)
    from("LICENSE")
}

tasks.assemble {
    dependsOn(devJar)
}

java {
    withSourcesJar()
}

loom {
    mods {
        create("glide") {
            sourceSet(bundled)
            sourceSet(main)
        }
    }

    runs {
        remove(getByName("client"))

        named("server") {
            sourceSet = bundled.name
        }
        create("datagenClient") {
            client()
            programArguments.add("--arcade-datagen")
            runDirectory = layout.projectDirectory.dir("run-datagen")
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "glide"
            from(components["java"])
            artifact(devJar)
        }
    }
}
