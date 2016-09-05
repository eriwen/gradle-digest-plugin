package com.eriwen.gradle

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE

class DigestPluginFunctionalTest extends Specification {
    @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
    }

    def "applying plugin registers digest task type"() {
        given:
        testProjectDir.newFile("file.js") << "text content"
        buildFile << """
            plugins {
                id 'com.eriwen.gradle.digest'
            }

            task digest(type: com.eriwen.gradle.Digest) {
            }
        """

        when:
        def result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments('digest')
            .build()

        then:
        result.task(":digest").outcome == UP_TO_DATE
    }
}
