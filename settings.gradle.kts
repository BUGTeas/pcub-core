rootProject.name = "PCUB-Core"

pluginManagement {
    repositories {
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        gradlePluginPortal()
    }
}

includeBuild("build-logic")

include("common")
include("bukkit")
include("velocity")

// 涉及 Geyser 底层的功能（不同平台下，依赖受不同的 relocate 影响而拆分）
include("geyser")
include("bukkit:geyser")
include("velocity:geyser")