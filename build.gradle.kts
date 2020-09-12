
import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.4.0"
    // gradle-intellij-plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
    id("org.jetbrains.intellij") version "0.4.22"
    // gradle-changelog-plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
    id("org.jetbrains.changelog") version "0.5.0"
    // detekt linter - read more: https://detekt.github.io/detekt/kotlindsl.html
    id("io.gitlab.arturbosch.detekt") version "1.12.0"
    // ktlint linter - read more: https://github.com/JLLeitschuh/ktlint-gradle
    id("org.jlleitschuh.gradle.ktlint") version "9.4.0"
}

repositories {
    jcenter()
    mavenCentral()
}

val pluginVersion = "1.0.2"

group = "io.github.markusmo3"
version = pluginVersion

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.12.0")
    compileOnly(kotlin("stdlib-jdk8"))
}

intellij {
    version = "2020.1"
    pluginName = "BetterMnemonics"
    downloadSources = true
    updateSinceUntilBuild = false
//    setPlugins("IdeaVIM:0.57")
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

tasks {
    patchPluginXml {
        changeNotes({ changelog.getLatest().toHTML() })
    }
    publishPlugin {
        dependsOn("patchChangelog")
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
