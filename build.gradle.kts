import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "com.eriwen"
version = "0.0.3"
description = "Digest sources to allow identification of files by their content"
val websiteUrl by extra { "https://github.com/eriwen/gradle-digest-plugin" }
val vcsUrl by extra { "https://github.com/eriwen/gradle-digest-plugin" }

plugins {
    id("com.gradle.build-scan") version "1.13.1"
    kotlin("jvm") version "1.2.50"
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "0.9.10"
    id("org.jetbrains.dokka") version "0.9.16"
    id("com.github.gradle-guides.site") version "0.1"
}

repositories {
    jcenter()
}

val kotlinVersion = "1.2.50"
val junitPlatformVersion = "1.1.0"
val spekVersion = "1.1.5"

dependencies {
    implementation(kotlin("stdlib-jdk8", kotlinVersion))
    implementation("commons-codec:commons-codec:1.10")

    testImplementation(kotlin("reflect", kotlinVersion))
    testImplementation(kotlin("test", kotlinVersion))
    testImplementation("org.jetbrains.spek:spek-api:$spekVersion") {
        exclude(group = "org.jetbrains.kotlin")
    }

    testRuntimeOnly("org.jetbrains.spek:spek-junit-platform-engine:$spekVersion") {
        exclude(group = "org.jetbrains.kotlin")
        exclude(group = "org.junit.platform")
    }

    testRuntimeOnly("org.junit.platform:junit-platform-launcher:$junitPlatformVersion") {
        because("Needed to run tests IDEs that bundle an older version")
    }
}

dependencyLocking {
    lockAllConfigurations()
}

buildScan {
    setTermsOfServiceUrl("https://gradle.com/terms-of-service")
    setTermsOfServiceAgree("yes")

    publishAlways()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

val dokka by tasks.getting(DokkaTask::class) {
    outputFormat = "html"
    outputDirectory = "$buildDir/javadoc"
    jdkVersion = 8
}

val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles Kotlin docs with Dokka"
    classifier = "javadoc"
    from(dokka)
}
artifacts.add("archives", dokkaJar)

val sourcesJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles sources JAR"
    classifier = "sources"
    from(java.sourceSets.getByName("main").allSource)
}
artifacts.add("archives", sourcesJar)

val jar by tasks.getting(Jar::class) {
    manifest.attributes.apply {
        put("Implementation-Title", "Gradle Digest Plugin")
        put("Implementation-Version", project.version)
        put("Built-By", System.getProperty("user.name"))
        put("Built-JDK", System.getProperty("java.version"))
        put("Built-Gradle", project.gradle.gradleVersion)
    }
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform {
        includeEngines("spek")
    }

    testLogging {
        events("PASSED", "FAILED", "SKIPPED")
    }
}

gradlePlugin {
    (plugins) {
        "digest" {
            id = "com.eriwen.gradle.digest"
            implementationClass = "com.eriwen.gradle.DigestPlugin"
        }
    }
}

pluginBundle {
    website = websiteUrl
    vcsUrl = vcsUrl
    description = project.description
    tags = listOf("asset-versioning", "caching", "md5", "sha256", "sha512")

    (plugins) {
        "digest" {
            id = "com.eriwen.gradle.digest"
            displayName = "Gradle Digest Plugin"
        }
    }
}

site {
    // This allows it to be picked up by GitHub Pages
    outputDir = file("$rootDir/docs")
    websiteUrl = websiteUrl
    vcsUrl = vcsUrl
}

publishing {
    publications.create("mavenJava", MavenPublication::class.java) {
        from(components.getByName("java"))
        artifact(dokkaJar)
        artifact(sourcesJar)

        pom {
            name.set("Gradle Digest plugin")
            description.set("Gradle plugin for digesting source files.")
            url.set("https://github.com/eriwen/gradle-digest-plugin")
            inceptionYear.set("2016")

            scm {
                url.set("https://github.com/eriwen/gradle-digest-plugin")
                connection.set("scm:https://eriwen@github.com/eriwen/gradle-digest-plugin.git")
                developerConnection.set("scm:git://github.com/eriwen/gradle-digest-plugin.git")
            }

            licenses {
                license {
                    name.set("The Apache Software License, Version 2.0")
                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    distribution.set("repo")
                }
            }

            developers {
                developer {
                    id.set("eriwen")
                    name.set("Eric Wendelin")
                    email.set("me@eriwen.com")
                }
            }
        }
    }
}

val install by tasks.creating {
    description = "Installs plugin to local repo"
    dependsOn("publishToMavenLocal")
}
