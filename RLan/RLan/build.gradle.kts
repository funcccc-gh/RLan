plugins {
    java
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "rlan"
version = "0.1.0"

repositories {
    maven { url = uri("https://maven.aliyun.com/repository/public") }
    maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
    mavenCentral()
}

dependencies {
    implementation("io.netty:netty-all:4.1.114.Final")
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("ch.qos.logback:logback-classic:1.5.12")
}

javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.fxml")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "rlan.Main"
}
