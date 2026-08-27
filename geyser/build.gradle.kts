plugins {
    id("pcub.base-conventions")
    id("io.freefair.lombok") version "8.6"
}

val geyserVersion = project.property("geyser-version") as String

dependencies {
    compileOnly(project(":common"))
    compileOnlyApi("org.geysermc.geyser:api:$geyserVersion-SNAPSHOT")
    compileOnly("org.geysermc.geyser:core:$geyserVersion-SNAPSHOT")
    // 显式依赖 Geyser 内置库
    compileOnly("org.cloudburstmc.fastutil.maps:object-boolean-maps:8.5.15-SNAPSHOT")
}

tasks {
    processResources {
        outputs.upToDateWhen { false }

        val name = rootProject.name
        val version = rootProject.version
        val geyserVersion = geyserVersion
        filesMatching("extension.yml") {
            expand(
                "name" to name,
                "version" to version,
                "geyserVersion" to geyserVersion
            )
        }
    }
}