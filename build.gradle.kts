plugins {
    id("pcub.base-conventions")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":bukkit", "shadow"))
    implementation(project(":velocity", "shadow"))

    // 涉及 Geyser 底层的功能（不同平台下，依赖受不同的 relocate 影响而拆分）
    implementation(project(":geyser")) // Geyser 扩展
    implementation(project(":bukkit:geyser", "shadow"))
    implementation(project(":velocity:geyser", "shadow"))
}

tasks {
    jar {
        dependsOn(shadowJar)
        archiveVersion.set("")
        archiveClassifier.set("unshaded")
    }
    shadowJar {
        archiveVersion.set("")
        archiveClassifier.set("")
    }
}