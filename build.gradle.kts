plugins {
    java
    id("org.springframework.boot") version "3.5.7"
    id("io.spring.dependency-management") version "1.1.7"
    id("pmd")
}

group = "com.kov"
version = "0.0.1-SNAPSHOT"
description = "voting-system"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.assertj:assertj-core:3.26.3")
    // Cucumber BDD (JUnit Platform)
    testImplementation("io.cucumber:cucumber-java:7.20.1")
    testImplementation("io.cucumber:cucumber-spring:7.20.1")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:7.20.1")
    testImplementation("org.junit.platform:junit-platform-suite:1.11.3")
    // For acceptance API testing (optional, MockMvc already included via starter-test)
    testImplementation("io.rest-assured:rest-assured:5.5.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Добавлена конфигурация PMD
pmd {
    toolVersion = "7.6.0"
    // Укажем кастомный ruleset, если он есть в config/pmd
    ruleSets = listOf()
    ruleSetFiles = files("${project.projectDir}/config/pmd/pmd-ruleset.xml")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Настройки для задач PMD: анализировать стандартные исходники
tasks.withType<org.gradle.api.plugins.quality.Pmd> {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    // ensure sources are picked up
    source = fileTree("src/main/java").apply { include("**/*.java") }
    // не давать падать сборке при найденных нарушениях — чтобы отчёты всегда генерировались
    ignoreFailures = true
}
