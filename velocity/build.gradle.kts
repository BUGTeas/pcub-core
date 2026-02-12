plugins {
    id("pcub.base-conventions")
}

repositories {
}

val geyserVersion = project.property("geyser-version") as String

dependencies {
    compileOnly(project(":common"))
    compileOnly(project(":velocity:geyser"))
}

tasks {
//    processResources {
//        outputs.upToDateWhen { false }
//        val name = rootProject.name
//        val version = rootProject.version
//        val geyserVersion = geyserVersion
//        filesMatching("extension.yml") {
//            expand(
//                "name" to name,
//                "version" to version,
//                "geyserVersion" to geyserVersion
//            )
//        }
//    }
    shadowJar {
        outputs.upToDateWhen { false }

        relocate("org.pcub.core.geyser", "org.pcub.core.velocity.geyser")
    }
}