plugins {
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.serialization") version "1.6.21"

    id("net.mamoe.mirai-console") version "2.12.0"
    id("net.mamoe.maven-central-publish") version "0.7.1"
}

group = "io.github.gnuf0rce"
version = "1.3.1"

mavenCentralPublish {
    useCentralS01()
    singleDevGithubProject("gnuf0rce", "debug-helper", "cssxsh")
    licenseFromGitHubProject("AGPL-3.0", "master")
    publication {
        artifact(tasks.getByName("buildPlugin"))
    }
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    compileOnly("net.mamoe:mirai-core:2.12.0")
    compileOnly("net.mamoe:mirai-core-utils:2.12.0")
    testImplementation(kotlin("test", "1.6.21"))
}

mirai {
    jvmTarget = JavaVersion.VERSION_11
}

tasks {
    test {
        useJUnitPlatform()
    }
}