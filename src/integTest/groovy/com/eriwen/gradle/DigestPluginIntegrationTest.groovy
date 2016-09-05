package com.eriwen.gradle

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class DigestPluginIntegrationTest extends Specification {
    @Rule
    TemporaryFolder temporaryFolder = new TemporaryFolder()
    final def project = ProjectBuilder.builder().withProjectDir(temporaryFolder.create()).build()

    def dsl(@DelegatesTo(Project) Closure closure) {
        closure.delegate = project
        closure()
    }

    def "adds digest task type"() {
        given:
        // FIXME(ew): put text file in a directory, this is annoyingly difficult
//        temporaryFolder.newFolder("dir").createNewFile()
        project.file("file.js") << "text content"

        dsl {
            apply plugin: DigestPlugin

            task digest(type: Digest) {
                include "**/*.js"
                dest "$project.buildDir/digest"
            }
        }

        when:
        def result = GradleRunner.create()
            .withProjectDir(temporaryFolder.root)
            .withArguments('digest')
            .build()

        then:
        result.task(":digest").outcome == SUCCESS

        and:
        new File("$project.buildDir/digest/file.js.md5").exists()
    }
}
