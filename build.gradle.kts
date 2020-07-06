import org.jetbrains.changelog.closure

group = "io.github.markusmo3"
version = "1.0.1"

plugins {
    java
    kotlin("jvm") version "1.3.72"
    id("org.jetbrains.intellij").version("0.4.21")

    // https://github.com/JetBrains/gradle-changelog-plugin
    id("org.jetbrains.changelog").version("0.3.2")
}

dependencies {
    testImplementation("io.mockk:mockk:1.9.3")
}

repositories {
    jcenter()
    mavenCentral()
}

intellij {
    version = "2019.2"
    pluginName = "BetterMnemonics"
    updateSinceUntilBuild = false
//    setPlugins("IdeaVIM:0.57")
}

changelog {
    version = "${project.version}"
    path = "${project.projectDir}/CHANGELOG.md"
    headerFormat = "[{0}]"
    headerArguments = listOf("${project.version}")
    itemPrefix = "-"
    keepUnreleasedSection = true
    unreleasedTerm = "Unreleased"
}

tasks {
    patchPluginXml {
        changeNotes(closure { changelog.getLatest().toHTML() })
    }
    publishPlugin {
        dependsOn("patchChangelog")
        token(System.getenv("ORG_GRADLE_PROJECT_intellijPublishToken"))
//        channels("beta")
    }
    compileJava {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }
    compileTestJava {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }
    compileKotlin {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
    compileTestKotlin {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
}
