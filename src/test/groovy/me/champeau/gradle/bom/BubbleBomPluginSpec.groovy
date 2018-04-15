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

    File settingsFile

    def setup() {
        settingsFile = testProjectDir.newFile('settings.gradle')
    }

    @Unroll
    def "forces dependency #group:#module to #expectedVersion using BOM (Kotlin DSL=#kotlin)"() {
        given:
        File buildFile = testProjectDir.newFile("build.gradle${kotlin ? '.kts' : ''}")
        settingsFile << "enableFeaturePreview('IMPROVED_POM_SUPPORT')"
        buildFile << """
plugins {
   id("me.champeau.bubble-bom")
}

repositories {
   jcenter()
}

configurations {
   create("conf")
}

dependencyManagement {
   importBom("org.springframework.boot:spring-boot-dependencies:2.0.1.RELEASE")
}

dependencies {
   "conf"("org.springframework.boot:spring-boot-starter-web")
   "conf"("com.googlecode.json-simple:json-simple:1.0")
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
        kotlin | group                        | module        | expectedVersion | appendix
        false  | 'org.yaml'                   | 'snakeyaml'   | '1.19'          | ''
        false  | 'com.googlecode.json-simple' | 'json-simple' | '1.1.1'         | ' (Mutated from 1.0 to 1.1.1 enforce BOM)'
        true   | 'org.yaml'                   | 'snakeyaml'   | '1.19'          | ''
        true   | 'com.googlecode.json-simple' | 'json-simple' | '1.1.1'         | ' (Mutated from 1.0 to 1.1.1 enforce BOM)'
    }
}
