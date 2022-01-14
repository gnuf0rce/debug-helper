plugins {
    kotlin("jvm") version "1.5.31"
    kotlin("plugin.serialization") version "1.5.31"

    id("net.mamoe.mirai-console") version "2.9.2"
    id("net.mamoe.maven-central-publish") version "0.7.0"
}

group = "io.github.gnuf0rce"
version = "1.2.0"

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
    maven(url = "https://maven.aliyun.com/repository/central")
    mavenCentral()
    maven(url = "https://maven.aliyun.com/repository/gradle-plugin")
    gradlePluginPortal()
}

dependencies {
    compileOnly("net.mamoe:mirai-core-jvm:2.9.2")
    testImplementation(kotlin("test", "1.5.31"))
}

kotlin {
    sourceSets {
        all {
//            languageSettings.useExperimentalAnnotation("net.mamoe.mirai.console.util.ConsoleExperimentalApi")
//            languageSettings.useExperimentalAnnotation("net.mamoe.mirai.utils.MiraiInternalApi")
//            languageSettings.useExperimentalAnnotation("net.mamoe.mirai.utils.MiraiExperimentalApi")
        }
    }
}

tasks {
    test {
        useJUnitPlatform()
    }
}