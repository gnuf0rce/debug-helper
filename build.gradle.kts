plugins {
    kotlin("jvm") version "1.7.20"
    kotlin("plugin.serialization") version "1.7.20"

    id("net.mamoe.mirai-console") version "2.13.0-RC2"
    id("me.him188.maven-central-publish") version "1.0.0-dev-3"
}

group = "io.github.gnuf0rce"
version = "1.3.5"

mavenCentralPublish {
    useCentralS01()
    singleDevGithubProject("gnuf0rce", "debug-helper", "cssxsh")
    licenseFromGitHubProject("AGPL-3.0")
    workingDir = System.getenv("PUBLICATION_TEMP")?.let { file(it).resolve(projectName) }
        ?: buildDir.resolve("publishing-tmp")
    publication {
        artifact(tasks["buildPlugin"])
    }
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    compileOnly("net.mamoe:mirai-core:2.13.0-RC2")
    compileOnly("net.mamoe:mirai-core-utils:2.13.0-RC2")
    implementation("io.ktor:ktor-client-okhttp:2.1.2") {
        exclude(group = "org.jetbrains.kotlin")
        exclude(group = "org.jetbrains.kotlinx")
        exclude(group = "org.slf4j")
    }
    implementation("io.ktor:ktor-client-encoding:2.1.2") {
        exclude(group = "org.jetbrains.kotlin")
        exclude(group = "org.jetbrains.kotlinx")
        exclude(group = "org.slf4j")
    }
    implementation("com.squareup.okhttp3:okhttp:4.10.0") {
        exclude(group = "org.jetbrains.kotlin")
        exclude(group = "org.jetbrains.kotlinx")
        exclude(group = "org.slf4j")
    }
    // Test
    testImplementation(kotlin("test"))
    testImplementation("org.slf4j:slf4j-simple:2.0.3")
    testImplementation("net.mamoe:mirai-logging-slf4j:2.13.0-RC2")
}

mirai {
    jvmTarget = JavaVersion.VERSION_11
}

tasks {
    test {
        useJUnitPlatform()
    }
}