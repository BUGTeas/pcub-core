plugins {
    id("java-library")
    id("com.gradleup.shadow")
}

repositories {
    maven("https://repo.opencollab.dev/main/")

    maven("https://maven.aliyun.com/repository/central")
    mavenCentral()
}

val targetJavaVersion = 21
java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }
}