plugins {
    id("org.jetbrains.kotlin.jvm")
    id("dev.architectury.loom")
    id("architectury-plugin")
    id("net.kyori.blossom")
}

architectury {
    common("neoforge", "fabric")
}

loom {
    silentMojangMappingsLicense()

    @Suppress("UnstableApiUsage")
    mixin {
        useLegacyMixinAp.set(true)
        defaultRefmapName.set("mixins.${project.name}.refmap.json")
    }
}

dependencies {
    // Mixin support
    compileOnly("net.fabricmc:sponge-mixin:0.16.3+mixin.0.8.7")

    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.officialMojangMappings())
    modImplementation("com.cobblemon:mod:${property("cobblemon_version")}") { isTransitive = false }

    testImplementation("org.junit.jupiter:junit-jupiter-api:${property("junit_version")}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${property("junit_version")}")
}

tasks.test {
    useJUnitPlatform()
}

sourceSets.main {
    blossom {
        kotlinSources {
            property("modid", project.properties["mod_id"].toString())
            property("version", project.version.toString())
        }
    }
}