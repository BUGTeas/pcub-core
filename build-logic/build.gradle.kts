plugins {
    `kotlin-dsl`
}

repositories {
    maven("https://maven.aliyun.com/repository/gradle-plugin")
    gradlePluginPortal()
}

dependencies {
    // version must be manually kept in sync with the one in root project settings.gradle.kts
    implementation("com.gradleup.shadow:shadow-gradle-plugin:9.3.1")

    // A nice no-conflict comment for patching in downgrading
}