plugins {
    val kotlinVersion = "1.5.21"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.7.1"
    id("net.mamoe.maven-central-publish") version "0.6.1"
}

group = "io.github.gnuf0rce"
version = "1.0.5"

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
    maven(url = "https://maven.aliyun.com/repository/releases")
    maven(url = "https://maven.aliyun.com/repository/public")
    mavenCentral()
    jcenter()
    maven(url = "https://maven.aliyun.com/repository/gradle-plugin")
    gradlePluginPortal()
}

dependencies {
    compileOnly("net.mamoe:mirai-core-jvm:2.7.1")
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
