/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson.
 */
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property

/**
 * Plugin for building JasperReports reports.
 */
class BuildReportsPlugin implements Plugin<Project> {

    @Override
    void apply(final Project project) {
        def config = project.extensions.create("buildReports", BuildReportsExtension)
        def buildReports = project.tasks.register("buildReports") {
            group = "build"
            inputs.dir config.sourceDir
            outputs.dir config.targetDir
            def mainSourceSet = project.getExtensions()
                    .getByType(JavaPluginExtension.class).getSourceSets().named("main").get()
            dependsOn mainSourceSet.getRuntimeClasspath()
            doLast {
                ant.lifecycleLogLevel = "INFO"
                ant.taskdef(name: "jrc", classname: "net.sf.jasperreports.ant.JRAntCompileTask",
                        classpath: mainSourceSet.getRuntimeClasspath().asPath)
                config.targetDir.get().mkdirs()
                ant.jrc(srcdir: config.sourceDir.get(), destdir: config.targetDir.get()) {
                    include(name: "**/*.jrxml")
                }
            }
        }
        project.configure(project) {
            project.afterEvaluate {
                project.getTasks().named("classes").get().finalizedBy(buildReports)
                project.getTasks().named("jar").get().dependsOn(buildReports)
                project.getTasks().named("compileTestJava").get().dependsOn(buildReports)
                project.getTasks().named("javadoc").get().dependsOn(buildReports)
                project.getTasks().stream()
                        .filter { it.getName().startsWith("run") }
                        .each { it.dependsOn(buildReports) }
                project.getTasks().stream()
                        .filter { (it.getName() == "domainJar") }
                        .each { it.dependsOn(buildReports) }
            }
        }
    }
}

abstract class BuildReportsExtension {
    /**
     * @return The reports source dir
     */
    abstract Property<File> getSourceDir()
    /**
     * @return The target dir for the compiled reports
     */
    abstract Property<File> getTargetDir()
}