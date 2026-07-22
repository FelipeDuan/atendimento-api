plugins {
	java
	checkstyle
	id("com.diffplug.spotless") version "7.0.4"
	id("org.springframework.boot") version "4.1.0"
	id("io.spring.dependency-management") version "1.1.7"
}

import org.springframework.boot.gradle.tasks.run.BootRun

group = "com.felipeduan"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.flywaydb:flyway-database-postgresql")
	implementation("org.springframework.boot:spring-boot-starter-aspectj")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-scalar:3.0.3")
	implementation("org.bouncycastle:bcprov-jdk18on:1.79")
	implementation("org.mapstruct:mapstruct:1.6.3")
	compileOnly("org.projectlombok:lombok")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	developmentOnly("org.springframework.boot:spring-boot-docker-compose")
	runtimeOnly("org.postgresql:postgresql")
	annotationProcessor("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")
	annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-redis-test")
	testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:testcontainers-junit-jupiter")
	testImplementation("org.testcontainers:testcontainers-postgresql")
	testCompileOnly("org.projectlombok:lombok")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testAnnotationProcessor("org.projectlombok:lombok")
}

checkstyle {
	toolVersion = "10.23.1"
	configDirectory.set(layout.projectDirectory.dir("config/checkstyle"))
	isIgnoreFailures = false
}

spotless {
	java {
		target("src/*/java/**/*.java")
		googleJavaFormat("1.35.0")
		removeUnusedImports()
		trimTrailingWhitespace()
		endWithNewline()
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
	environment("META_APP_SECRET", "test-meta-app-secret")
	environment("META_VERIFY_TOKEN", "test-verify-token")
	testLogging {
		events("passed", "failed", "skipped")
		showStandardStreams = true
	}
}

fun carregarDotEnv(arquivo: File): Map<String, String> {
	if (!arquivo.isFile) {
		return emptyMap()
	}

	return arquivo.readLines()
		.map { it.trim() }
		.filter { it.isNotEmpty() && !it.startsWith("#") }
		.mapNotNull { linha ->
			val separador = linha.indexOf('=')
			if (separador <= 0) {
				null
			} else {
				linha.substring(0, separador) to linha.substring(separador + 1)
			}
		}
		.toMap()
}

tasks.named<BootRun>("bootRun") {
	val dotEnv = carregarDotEnv(layout.projectDirectory.file(".env").asFile)
	dotEnv.forEach { (nome, valor) -> environment(nome, valor) }
}
