import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.junit.platform.console.options.Details
import org.junit.platform.gradle.plugin.EnginesExtension
import org.junit.platform.gradle.plugin.FiltersExtension
import org.junit.platform.gradle.plugin.JUnitPlatformExtension

buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath("org.junit.platform:junit-platform-gradle-plugin:1.0.0")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:0.9.15")
    }
}
apply {
    plugin("org.junit.platform.gradle.plugin")
    plugin("org.jetbrains.dokka")
}

group = "com.eriwen"
version = "0.0.3"

plugins {
    id("com.gradle.build-scan") version "1.12.1"
    kotlin("jvm") version "1.2.21"
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "0.9.10"
}

repositories {
    jcenter()
}

val kotlinVersion = "1.2.21"
val junitPlatformVersion = "1.0.0"
val spekVersion = "1.1.5"

dependencies {
    implementation(kotlin("stdlib-jre8", kotlinVersion))
    implementation("commons-codec:commons-codec:1.10")

    testImplementation(kotlin("reflect", kotlinVersion))
    testImplementation(kotlin("test", kotlinVersion))
    testImplementation("org.jetbrains.spek:spek-api:$spekVersion") {
        exclude(group = "org.jetbrains.kotlin")
    }

    testRuntimeOnly("org.jetbrains.spek:spek-junit-platform-engine:$spekVersion") {
        exclude(group = "org.jetbrains.kotlin")
        exclude("org.junit.platform")
    }
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:$junitPlatformVersion")
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

configure<JUnitPlatformExtension> {
    platformVersion = junitPlatformVersion
    details = Details.TREE
    filters {
        engines {
            include("spek")
        }
    }
}

fun JUnitPlatformExtension.filters(setup: FiltersExtension.() -> Unit) {
    when (this) {
        is ExtensionAware -> extensions.getByType(FiltersExtension::class.java).setup()
        else -> throw Exception("${this::class} must be an instance of ExtensionAware")
    }
}

fun FiltersExtension.engines(setup: EnginesExtension.() -> Unit) {
    when (this) {
        is ExtensionAware -> extensions.getByType(EnginesExtension::class.java).setup()
        else -> throw Exception("${this::class} must be an instance of ExtensionAware")
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
    website = "https://github.com/eriwen/gradle-digest-plugin"
    vcsUrl = "https://github.com/eriwen/gradle-digest-plugin"
    description = "Digest sources to allow identification of files by their content"
    tags = listOf("asset-versioning", "caching", "md5", "sha256", "sha512")

    (plugins) {
        "digest" {
            id = "com.eriwen.gradle.digest"
            displayName = "Gradle Digest Plugin"
        }
    }
}

publishing {
    publications.create("mavenJava", MavenPublication::class.java) {
        from(components.getByName("java"))
        artifact(dokkaJar)
        artifact(sourcesJar)

        pom.withXml {
            val root = asNode()
            root.appendNode("name", "Gradle Digest plugin")
            root.appendNode("description", "Gradle plugin for digesting source files.")
            root.appendNode("url", "https://github.com/eriwen/gradle-digest-plugin")
            root.appendNode("inceptionYear", "2016")

            val scm = root.appendNode("scm")
            scm.appendNode("url", "https://github.com/eriwen/gradle-digest-plugin")
            scm.appendNode("connection", "scm:https://eriwen@github.com/eriwen/gradle-digest-plugin.git")
            scm.appendNode("developerConnection", "scm:git://github.com/eriwen/gradle-digest-plugin.git")

            val license = root.appendNode("licenses").appendNode("license")
            license.appendNode("name", "The Apache Software License, Version 2.0")
            license.appendNode("url", "http://www.apache.org/licenses/LICENSE-2.0.txt")
            license.appendNode("distribution", "repo")

            val developers = root.appendNode("developers")
            val developer = developers.appendNode("developer")
            developer.appendNode("id", "eriwen")
            developer.appendNode("name", "Eric Wendelin")
            developer.appendNode("email", "me@eriwen.com")
        }
    }
}

val install by tasks.creating {
    description = "Installs plugin to local repo"
    dependsOn("publishToMavenLocal")
}
