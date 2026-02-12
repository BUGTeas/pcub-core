plugins {
    id("pcub.base-conventions")
}

dependencies {
    api(project(":geyser"))
}

tasks {
    shadowJar {
        outputs.upToDateWhen { false }

        relocate("org.pcub.core.geyser", "org.pcub.core.velocity.geyser")

        relocate("it.unimi.dsi.fastutil", "org.geysermc.geyser.platform.velocity.shaded.it.unimi.dsi.fastutil")
    }
}