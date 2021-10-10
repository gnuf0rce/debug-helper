plugins {
    kotlin("jvm") version "1.5.21"
    kotlin("plugin.serialization") version "1.5.21"

    id("net.mamoe.mirai-console") version "2.8.0-M1"
    id("net.mamoe.maven-central-publish") version "0.6.1"
}

group = "io.github.gnuf0rce"
version = "1.1.0"

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
    maven(url = "https://maven.aliyun.com/repository/public")
    mavenCentral()
    maven(url = "https://maven.aliyun.com/repository/gradle-plugin")
    gradlePluginPortal()
}

dependencies {
    compileOnly("net.mamoe:mirai-core-jvm:2.7.1")
    testImplementation(kotlin("test-junit5"))
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