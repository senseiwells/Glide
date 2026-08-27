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

val modVersion = "0.2.0"
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

dependencies {
    minecraft(libs.minecraft)
    implementation(libs.fabric.loader)
    implementation(libs.fabric.api)
    implementation(libs.fabric.kotlin)

    implementation(libs.arcade)
    implementation(libs.arcade.datagen)
    implementation(libs.polymer.core)
}

tasks {
    processResources {
        inputs.property("version", project.version)
        filesMatching("fabric.mod.json") {
            expand(mutableMapOf(
                "version" to modVersion,
                "minecraft_dependency" to libs.versions.minecraft.get(),
                "fabric_loader_dependency" to libs.versions.fabric.loader.get(),
                "fabric_api_dependency" to libs.versions.fabric.api.get(),
                "fabric_kotlin_dependency" to libs.versions.fabric.kotlin.get()
            ))
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