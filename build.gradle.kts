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

val modVersion = "0.1.0"
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
    mavenCentral()
}

dependencies {
    minecraft(libs.minecraft)
    @Suppress("UnstableApiUsage")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${libs.versions.parchment.get()}@zip")
    })
    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.api)
    modImplementation(libs.fabric.kotlin)

    modImplementation(libs.arcade)
    modImplementation(libs.polymer.core)
    modImplementation(libs.polymer.virtual.entity)

    modImplementation(libs.arcade.datagen)

    include(implementation(libs.zip4j.get())!!)
}

tasks {
    processResources {
        inputs.property("version", project.version)
        filesMatching("fabric.mod.json") {
            expand(mutableMapOf("version" to project.version))
        }
    }

    jar {
        from("LICENSE")
    }
}

java {
    withSourcesJar()
}

loom {
    runs {
        create("datagenClient") {
            client()
            programArgs("--arcade-datagen")
            runDir = "run-datagen"
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "glide"
            from(components["java"])
        }
    }
}