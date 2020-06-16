group = "io.github.markusmo3"
version = "1.0-SNAPSHOT"

plugins {
    java
    kotlin("jvm") version "1.3.41"
    id("org.jetbrains.intellij").version("0.4.9")
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
    updateSinceUntilBuild = false
//    setPlugins("IdeaVIM:0.57")
//    publishPlugin {
//        username publishUsername
//                token publishToken
//    }
}

tasks {
//    registerAdditionalTasks()
//    publishPlugin {
//        token(project.property("publishPluginToken"))
//        channels("beta")
//    }
    compileJava {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }
    compileTestJava {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }
    compileKotlin {
//        dependsOn(generateGrammarSource)
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
