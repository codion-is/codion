/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
        def config = project.extensions.create('buildReports', BuildReportsExtension)
        def buildReports = project.task('buildReports') {
            group = 'build'
            inputs.dir config.sourceDir
            outputs.dir config.targetDir
            doLast {
                def javaPlugin = project.getExtensions().getByType(JavaPluginExtension.class)
                def main = javaPlugin.getSourceSets().findByName('main')
                ant.lifecycleLogLevel = 'INFO'
                ant.taskdef(name: 'jrc', classname: 'net.sf.jasperreports.ant.JRAntCompileTask',
                        classpath: main.getRuntimeClasspath().asPath)
                config.targetDir.get().mkdirs()
                ant.jrc(srcdir: config.sourceDir.get(), destdir: config.targetDir.get()) {
                    include(name: '**/*.jrxml')
                }
            }
        }
        project.configure(project) {
            project.afterEvaluate {
                project.getTasks().findByName('classes').finalizedBy(buildReports)
                project.getTasks().findByName('jar').dependsOn(buildReports)
                project.getTasks().findByName('test').dependsOn(buildReports)
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