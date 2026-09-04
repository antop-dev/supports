plugins {
    val kotlinVersion = "2.2.20"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("plugin.jpa") version kotlinVersion
    id("org.springframework.boot") version "4.0.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jlleitschuh.gradle.ktlint") version "13.1.0"
}

group = "ai.antop"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Database - SQLite
    implementation("org.xerial:sqlite-jdbc:3.50.1.0")
    implementation("org.hibernate.orm:hibernate-community-dialects")

    // Flyway (Spring Boot 4는 Flyway 자동 설정이 spring-boot-flyway 모듈로 분리됨)
    implementation("org.springframework.boot:spring-boot-flyway")
    implementation("org.flywaydb:flyway-core")

    // CSRF 방어 + 비밀번호 해시(BCrypt). 로그인 여부 판단은 AdminAuthInterceptor 가 계속 담당한다.
    implementation("org.springframework.boot:spring-boot-starter-security")

    // ULID
    implementation("com.github.f4b6a3:ulid-creator:5.2.3")

    // WYSIWYG HTML 새니타이징(XSS 방지)
    implementation("org.jsoup:jsoup:1.18.3")

    // Slack (Incoming Webhook + Block Kit)
    implementation("com.slack.api:slack-api-client:1.51.0")
    implementation("com.slack.api:slack-api-model-kotlin-extension:1.51.0")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

// JPA entities: make Hibernate-managed classes open and provide no-arg constructor
allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// 프로파일은 실행 시점에 정한다(application.yml 에 active 를 두지 않는다).
// 로컬 개발 편의를 위해 bootRun 만 local 을 붙인다. 배포는 CI/CD 가 --spring.profiles.active 로 준다.
// 커맨드라인 인자가 시스템 프로퍼티보다 우선하므로, --args 로 다른 프로파일을 줘도 그쪽이 이긴다.
tasks.bootRun {
    systemProperty("spring.profiles.active", "local")
}

ktlint {
    version.set("1.5.0")
}
