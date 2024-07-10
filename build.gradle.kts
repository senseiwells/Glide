plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.9.21"
    id("fabric-loom")
    `maven-publish`
    java
}

val modVersion: String by project

val mcVersion: String by project
val parchmentVersion: String by project
val loaderVersion: String by project
val fabricVersion: String by project
val fabricKotlinVersion: String by project

val arcadeVersion: String by project

val zip4jVersion: String by project

version = "${modVersion}+mc${mcVersion}"
group = "me.senseiwells"

repositories {
    // mavenLocal()
    maven("https://maven.parchmentmc.org/")
    maven("https://jitpack.io")
    maven("https://ueaj.dev/maven")
    maven("https://maven.nucleoid.xyz")
    mavenCentral()
}

@Suppress("UnstableApiUsage")
dependencies {
    minecraft("com.mojang:minecraft:${mcVersion}")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${parchmentVersion}@zip")
    })
    modImplementation("net.fabricmc:fabric-loader:${loaderVersion}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${fabricVersion}")

    modImplementation("net.fabricmc:fabric-language-kotlin:${fabricKotlinVersion}")

    include(modImplementation("com.github.CasualChampionships:arcade:${arcadeVersion}")!!)

    include(modImplementation("me.lucko:fabric-permissions-api:0.2-SNAPSHOT")!!)

    include(implementation("net.lingala.zip4j:zip4j:${zip4jVersion}")!!)
}

tasks {
    processResources {
        inputs.property("version", modVersion)
        filesMatching("fabric.mod.json") {
            expand(mutableMapOf("version" to modVersion))
        }
    }

    jar {
        from("LICENSE")
    }

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                from(project.components.getByName("java"))
            }
        }
    }
}

java {
    withSourcesJar()
}