plugins {
    id("pcub.base-conventions")
}

repositories {
}

val geyserVersion = project.property("geyser-version") as String

dependencies {
    compileOnlyApi("org.geysermc.geyser:api:$geyserVersion-SNAPSHOT")

    // Use JUnit Jupiter for testing.
    testImplementation(libs.junit.jupiter)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    named<Test>("test") {
        // Use JUnit Platform for unit tests.
        useJUnitPlatform()
    }
}