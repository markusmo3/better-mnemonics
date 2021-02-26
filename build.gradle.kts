import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.changelog.closure
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.71")
    }
}

plugins {
    java
    kotlin("jvm") version "1.4.0"
    // gradle-intellij-plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
    id("org.jetbrains.intellij") version "0.7.2"
    // gradle-changelog-plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
    id("org.jetbrains.changelog") version "1.1.2"
    // detekt linter - read more: https://detekt.github.io/detekt/kotlindsl.html
    id("io.gitlab.arturbosch.detekt") version "1.15.0"
    // ktlint linter - read more: https://github.com/JLLeitschuh/ktlint-gradle
    id("org.jlleitschuh.gradle.ktlint") version "10.0.0"
}

repositories {
    jcenter()
    mavenCentral()
}

val pluginVersion = "1.0.3"

group = "io.github.markusmo3"
version = pluginVersion

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.12.0")
    compileOnly(kotlin("stdlib-jdk8"))
}

intellij {
    version = "2020.3"
    pluginName = "BetterMnemonics"
    downloadSources = true
    updateSinceUntilBuild = false
}

// Configure detekt plugin.
// Read more: https://detekt.github.io/detekt/kotlindsl.html
detekt {
    config = files("./detekt-config.yml")
    buildUponDefaultConfig = true

    reports {
        html.enabled = false
        xml.enabled = false
        txt.enabled = false
    }
}

changelog {
    version = pluginVersion
    groups = listOf()
}

tasks {
    patchPluginXml {
        closure { changelog.has(pluginVersion) }
        changeNotes(closure { changelog.get(pluginVersion).toHTML() })
    }
    publishPlugin {
        token(System.getenv("ORG_GRADLE_PROJECT_intellijPublishToken"))
        channels(pluginVersion.split('-').getOrElse(1) { "default" }.split('.').first())
    }
    runIde {
        jvmArgs = listOf("-XX:+UnlockDiagnosticVMOptions")
    }


    // Set the compatibility versions to 1.8
    withType<JavaCompile> {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }
    listOf("compileKotlin", "compileTestKotlin").forEach {
        getByName<KotlinCompile>(it) {
            kotlinOptions {
                jvmTarget = "1.8"
                freeCompilerArgs = listOf("-Xjvm-default=enable")
            }
        }
    }
    withType<Detekt> {
        jvmTarget = "1.8"
    }
}
