plugins {
    id("pcub.base-conventions")
}

dependencies {
    api(project(":geyser"))
}

tasks {
    shadowJar {
        outputs.upToDateWhen { false }

        relocate("org.pcub.core.geyser", "org.pcub.core.bukkit.geyser")

        relocate("net.kyori", "org.geysermc.geyser.platform.spigot.shaded.net.kyori") {
            exclude("net.kyori.adventure.text.logger.slf4j.ComponentLogger")
        }
        relocate("it.unimi.dsi.fastutil", "org.geysermc.geyser.platform.spigot.shaded.it.unimi.dsi.fastutil")
    }
}