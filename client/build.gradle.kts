plugins {
	java
	id("org.springframework.boot") version "3.4.4"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "io.github.cue.clipboardbridge"
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
	implementation("org.springframework.boot:spring-boot-starter-websocket")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")

	// testImplementation("org.springframework.boot:spring-boot-starter-test")

}

// tasks.withType<Test> {
// 	useJUnitPlatform()
// }

springBoot {
    mainClass.set("io.github.cue.clipboardbridge.client.presentation.ClientApplication")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<ProcessResources> {
    filteringCharset = "UTF-8"
}

tasks.withType<JavaExec> {
    systemProperty("file.encoding", "UTF-8")
    jvmArgs = listOf("-Dfile.encoding=UTF-8", "-Dconsole.encoding=UTF-8")
}

tasks.withType<org.springframework.boot.gradle.tasks.run.BootRun> {
    systemProperty("file.encoding", "UTF-8")
    jvmArgs = listOf("-Dfile.encoding=UTF-8", "-Dconsole.encoding=UTF-8")
}
