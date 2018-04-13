package me.champeau.gradle.bom

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.VersionConstraint

open class BomExtension(val project: Project, val configuration: Configuration) {

    val forcedDependencies: MutableList<ForcedDependency> = mutableListOf()

    fun importBom(notation: Any) {
        val dependency = project.dependencies.create(notation)
        configuration.dependencies.add(dependency)
        inspectBom(dependency.group!!, dependency.name)
    }

    private
    fun inspectBom(group: String, name: String) = project.run {
        dependencies.components {
            withModule("$group:$name") {
                allVariants {
                    withDependencyConstraints {
                        mapTo(forcedDependencies) { ForcedDependency(it.group, it.name, it.versionConstraint) }
                    }
                }
            }
        }
    }

    internal
    fun resolve(): Unit {
        configuration.resolve()
    }
}


data class ForcedDependency(val group: String, val name: String, val version: VersionConstraint)