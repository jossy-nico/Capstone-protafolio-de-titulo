
plugins {
    java
    id("org.springframework.boot") version "4.0.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
description = "Demo project for Spring Boot"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
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

    // ==========================================
    // SPRING BOOT
    // ==========================================

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")

    // ==========================================
    // SEGURIDAD
    // ==========================================

    implementation("org.springframework.boot:spring-boot-starter-security")

    // JWT: emisión y validación de tokens
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")

    // ==========================================
    // BASE DE DATOS
    // ==========================================

    runtimeOnly("org.postgresql:postgresql")

    // ==========================================
    // LOMBOK
    // ==========================================

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // ==========================================
    // PRUEBAS
    // ==========================================

    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
