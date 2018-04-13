package me.champeau.gradle.bom

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

class BubbleBomPluginSpec extends Specification {
    @Rule
    final TemporaryFolder testProjectDir = new TemporaryFolder()

    File buildFile, settingsFile

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        settingsFile = testProjectDir.newFile('settings.gradle')
    }

    @Unroll
    def "forces dependency versions using BOM"() {
        given:
        settingsFile << "enableFeaturePreview('IMPROVED_POM_SUPPORT')"
        buildFile << """
plugins {
   id("me.champeau.bubble-bom")
}

repositories {
   jcenter()
}

configurations {
   conf
}

dependencyManagement {
   importBom "org.springframework.boot:spring-boot-dependencies:2.0.1.RELEASE"
}

dependencies {
   conf "org.springframework.boot:spring-boot-starter-web"
   conf "com.googlecode.json-simple:json-simple:1.0"
}    
"""

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("dependencyInsight", "--configuration", "conf", "--dependency", module)
                .withPluginClasspath()
                .build()

        then:
        result.output.contains """$group:$module:$expectedVersion$appendix
   variant "runtime" [
      org.gradle.usage = java-runtime (not requested)
   ]"""
        result.task(":dependencyInsight").outcome == TaskOutcome.SUCCESS

        where:
        group                        | module        | expectedVersion | appendix
        'org.yaml'                   | 'snakeyaml'   | '1.19'          | ''
        'com.googlecode.json-simple' | 'json-simple' | '1.1.1'         | ' (Mutated from 1.0 to 1.1.1 enforce BOM)'
    }
}
