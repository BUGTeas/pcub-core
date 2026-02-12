plugins {
    id("pcub.base-conventions")
}

repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "spigotmc-repo"
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }
    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
//    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly(project(":common"))
    compileOnly(project(":bukkit:geyser"))
}

tasks {
    processResources {
        outputs.upToDateWhen { false }

        val name = rootProject.name
        val version = rootProject.version
        filesMatching("plugin.yml") {
            expand(
                "name" to name,
                "version" to version
            )
        }
    }
    shadowJar {
        outputs.upToDateWhen { false }

        relocate("org.pcub.core.geyser", "org.pcub.core.bukkit.geyser")
    }
}