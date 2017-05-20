package com.eriwen.gradle

import org.apache.commons.codec.digest.DigestUtils
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.NO_SOURCE
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class DigestPluginFunctionalTest extends Specification {
    @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        buildFile << """
            plugins {
                id 'com.eriwen.gradle.digest'
            }
        """
    }

    def "applying plugin registers digest task type"() {
        given:
        buildFile << "task digest(type: com.eriwen.gradle.Digest) {}"

        when:
        BuildResult result = execute("digest")

        then:
        result.task(":digest").outcome == NO_SOURCE
    }

    def "hashes files using MD5 algorithm by default"() {
        given:
        String jsContent = "text content"
        String projectPath = testProjectDir.root.absolutePath
        testProjectDir.newFile("file.js") << jsContent

        buildFile << """
            task digest(type: com.eriwen.gradle.Digest) {
                source "${projectPath}"
                dest "${projectPath}/build"
            }
"""

        when:
        BuildResult result = execute("digest")

        then:
        result.task(":digest").outcome == SUCCESS
        new File("${projectPath}/build/${DigestUtils.md5Hex(jsContent)}-file.js").text == jsContent
        new File("${projectPath}/build/file.js.md5").text == DigestUtils.md5Hex(jsContent)
    }

    def "nested source directory structure is maintained"() {
        given:
        testProjectDir.newFolder("foo", "bar")
        testProjectDir.newFile("foo/quux.js") << "quux"
        testProjectDir.newFile("foo/bar/baz.js") << "baz"
        String projectPath = testProjectDir.root.absolutePath

        buildFile << """
            task digest(type: com.eriwen.gradle.Digest) {
                source = "${projectPath}/foo"
                dest = "${projectPath}/build"
            }
"""

        when:
        BuildResult result = execute("digest")

        then:
        result.task(":digest").outcome == SUCCESS
        new File("${projectPath}/build/quux.js.md5").exists()
        new File("${projectPath}/build/bar/baz.js.md5").exists()
    }

    def "digest algorithm is configurable"() {
        given:
        testProjectDir.newFolder("path")
        testProjectDir.newFile("path/a.js") << "a"
        String projectPath = testProjectDir.root.absolutePath

        buildFile << """
            task digest(type: com.eriwen.gradle.Digest) {
                source "${projectPath}/path"
                dest "${projectPath}/build"
                algorithm = "SHA1"
            }
"""

        when:
        BuildResult result = execute("digest")

        then:
        result.task(":digest").outcome == SUCCESS
        new File("${projectPath}/build/a.js.sha1").exists()
        new File("${projectPath}/build/86f7e437faa5a7fce15d1ddcb9eaeaea377667b8-a.js").exists()
    }

    private BuildResult execute(String arguments) {
        GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments(arguments)
            .withPluginClasspath()
            .build()
    }
}
