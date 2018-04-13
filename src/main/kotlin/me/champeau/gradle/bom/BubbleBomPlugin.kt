package me.champeau.gradle.bom

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencyMetadata
import org.gradle.api.artifacts.ExternalModuleDependency

class BubbleBomPlugin : Plugin<Project> {

    lateinit
    var extension: BomExtension

    override fun apply(project: Project): Unit = project.run {

        val bomConfig = configurations.detachedConfiguration()
        extension = extensions.create(BomExtension::class.java, "dependencyManagement", BomExtension::class.java, project, bomConfig)

        mutateDirectDependencies()
        mutateComponentsForDependency()

        afterEvaluate {
            extension.resolve()
        }
    }

    private
    fun Project.mutateDirectDependencies() {
        configurations.all {
            incoming.beforeResolve {
                dependencies.all {
                    val forcedDependencies = extension.forcedDependencies
                    if (this is ExternalModuleDependency) {
                        forcedDependencies.forEach { forced ->
                            if (group == forced.group && name == forced.name) {
                                because("Mutated from ${version?:"no version"} to ${forced.version} enforce BOM")
                                version {
                                    prefer(forced.version.preferredVersion)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private
    fun Project.mutateComponentsForDependency() {
        dependencies.components.all {
            val forcedDependencies = extension.forcedDependencies
            allVariants {
                withDependencies {
                    mutateDependencies(forcedDependencies)
                }
                withDependencyConstraints {
                    forEach {
                        mutateDependencies(forcedDependencies)
                    }
                }
            }
        }
    }

    private
    fun Collection<DependencyMetadata<*>>.mutateDependencies(forcedDependencies: MutableList<ForcedDependency>) {
        forEach {
            forcedDependencies.forEach { forced ->
                if (it.group == forced.group && it.name == forced.name) {
                    it.because("Mutated from ${it.versionConstraint} to ${forced.version} to enforce BOM")
                    it.version {
                        prefer(forced.version.preferredVersion)
                    }
                }
            }
        }
    }

}