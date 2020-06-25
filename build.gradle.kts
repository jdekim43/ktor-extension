import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Date

plugins {
    kotlin("jvm") version "1.3.70"
    `maven-publish`
    id("com.jfrog.bintray") version "1.8.4"
}

val artifactName = "ktor-extension"
val artifactGroup = "kr.jadekim"
val artifactVersion = "1.0.1"
group = artifactGroup
version = artifactVersion

repositories {
    jcenter()
    mavenCentral()
    maven("https://dl.bintray.com/jdekim43/maven")
}

dependencies {
    val commonApiServerVersion: String by project
    val jLoggerVersion: String by project
    val ktorVersion: String by project
    val jacksonVersion: String by project

    implementation(kotlin("stdlib-jdk8"))

    implementation("kr.jadekim:j-logger:$jLoggerVersion")
    api("kr.jadekim:common-api-server:$commonApiServerVersion")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    api("io.ktor:ktor-server-core:$ktorVersion")
    compileOnly("io.ktor:ktor-auth:$ktorVersion")
}

tasks.withType<KotlinCompile> {
    val jvmTarget: String by project

    kotlinOptions.jvmTarget = jvmTarget
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.getByName("main").allSource)
}

publishing {
    publications {
        create<MavenPublication>("lib") {
            groupId = artifactGroup
            artifactId = artifactName
            version = artifactVersion
            from(components["java"])
            artifact(sourcesJar)
        }
    }
}

bintray {
    user = System.getenv("BINTRAY_USER")
    key = System.getenv("BINTRAY_KEY")

    publish = true

    setPublications("lib")

    pkg.apply {
        repo = "maven"
        name = rootProject.name
        setLicenses("MIT")
        setLabels("kotlin", "ktor")
        vcsUrl = "https://github.com/jdekim43/ktor-extension.git"
        version.apply {
            name = artifactVersion
            released = Date().toString()
        }
    }
}