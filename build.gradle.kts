plugins {
    id("pcub.product-conventions")
}

// 用于构建通用 JAR

dependencies {
    runtimeOnly(project(":bukkit", "shadow"))
    runtimeOnly(project(":velocity", "shadow"))
    runtimeOnly(project(":geyser"))
}

tasks {
    jar {
        archiveVersion.set("")
        archiveClassifier.set("unshaded")
    }
}