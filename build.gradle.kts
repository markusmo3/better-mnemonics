group = "io.github.markusmo3"
version = "1.0.0-SNAPSHOT"

plugins {
    java
    kotlin("jvm") version "1.3.72"
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
}

tasks {
    publishPlugin {
        token(System.getenv("ORG_GRADLE_PROJECT_intellijPublishToken"))
        channels("beta")
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
