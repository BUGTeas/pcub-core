plugins {
    id("pcub.base-conventions")
}

tasks {
    shadowJar {
        archiveBaseName.set(rootProject.name)
        archiveVersion.set(if (project.name == rootProject.name) "" else project.name)
        archiveClassifier.set("")
    }
    build {
        dependsOn(shadowJar)
    }
}