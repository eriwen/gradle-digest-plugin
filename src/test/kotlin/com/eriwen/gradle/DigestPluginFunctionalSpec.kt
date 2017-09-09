package com.eriwen.gradle

import org.apache.commons.codec.digest.DigestUtils
import org.gradle.internal.impldep.org.junit.rules.TemporaryFolder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DigestPluginFunctionalSpec : Spek({
    describe("DigestPlugin") {
        context("with plugin applied") {
            val testProjectDir = TemporaryFolder()
            testProjectDir.create()
            val projectPath = testProjectDir.root.absolutePath
            val buildFile: File = testProjectDir.newFile("build.gradle")
            buildFile.writeText("""
                plugins {
                    id 'com.eriwen.gradle.digest'
                }

                task digest(type: com.eriwen.gradle.Digest) {
                    source "$projectPath/foo"
                    dest "$projectPath/build"
                }
            """)

            fun execute(arguments: String, projectDir: TemporaryFolder): BuildResult {
                return GradleRunner.create()
                        .withProjectDir(projectDir.root)
                        .withArguments(arguments)
                        .withPluginClasspath()
                        .build()
            }

            context("with sources in nested directories") {
                val jsContent = "text content"
                testProjectDir.newFolder("foo", "bar")
                testProjectDir.newFile("foo/file.js").writeText(jsContent)
                testProjectDir.newFile("foo/bar/baz.js").writeText("baz")

                it("hashes files using MD5 algorithm by default") {
                    val buildResult: BuildResult = execute("digest", testProjectDir)

                    assertEquals(TaskOutcome.SUCCESS, buildResult.task(":digest")!!.outcome)
                    assertEquals(jsContent, File("$projectPath/build/${DigestUtils.md5Hex(jsContent)}-file.js").readText())
                    assertEquals(DigestUtils.md5Hex(jsContent), File("$projectPath/build/file.js.md5").readText())

                    // ... it maintains source directory structure
                    assertTrue(File("$projectPath/build/bar/baz.js.md5").exists())
                }
            }

            context("with custom digest algorithm") {
                testProjectDir.newFolder("path")
                testProjectDir.newFile("path/a.js").writeText("a")

                buildFile.appendText("""
                    task digestSha1(type: com.eriwen.gradle.Digest) {
                        source "$projectPath/path"
                        dest "$projectPath/build"
                        algorithm = "SHA1"
                    }
                """)

                it("uses desired algorithm if available") {
                    val buildResult: BuildResult = execute("digestSha1", testProjectDir)

                    assertEquals(TaskOutcome.SUCCESS, buildResult.task(":digestSha1")!!.outcome)
                    assertTrue(File("$projectPath/build/a.js.sha1").exists())
                    assertTrue(File("$projectPath/build/86f7e437faa5a7fce15d1ddcb9eaeaea377667b8-a.js").exists())
                }
            }
        }
    }
})
