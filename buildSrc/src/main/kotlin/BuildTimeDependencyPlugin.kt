import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension

class BuildTimeDependencyPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("buildTimeDependency") {
            val catalogs = project.extensions.getByType(VersionCatalogsExtension::class.java)
            val libs = catalogs.named("libs")
            val dep = libs.findLibrary("findsecbugs-gradle-plugin").get()

            project.dependencies.add("spotbugsPlugins", dep)
        }
    }
}
