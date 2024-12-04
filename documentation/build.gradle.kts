plugins {
    id("org.asciidoctor.jvm.convert") version "4.0.3"
}

val documentationVersion = project.version.toString().replace("-SNAPSHOT", "")
val documentationDir = documentationVersion

tasks.register("copyModuleDependencyGraphs") {
    group = "documentation"
    description = "Copies the module dependency graphs to the asciidoc images folder"
    doLast {
        frameworkModules().forEach { module ->
            val moduleDir = module.projectDir
            var graphFilePath = "${moduleDir}/build/reports/dependency-graph/dependency-graph.svg"
            graphFilePath = graphFilePath.replace(rootDir.toString(), "").substring(1)
            ant.withGroovyBuilder {
                "copy"("todir" to project.layout.buildDirectory.dir("asciidoc/images/modules").get()) {
                    "fileset"("dir" to rootDir, "includes" to graphFilePath)
                }
            }
        }
    }
}

tasks.register("generateI18nValuesPage") {
    group = "documentation"
    description = "Generates the i18n asciidoc page"
    doLast {
        val file = file("src/docs/asciidoc/technical/i18n-values.adoc")
        val moduleFiles = LinkedHashMap<String, List<String>>()
        frameworkModules().forEach { module ->
            val files = module.sourceSets.main.get().resources.matching {
                include("**/*.properties")
            }.files.map { it.toString() }.sorted()
            if (files.isNotEmpty()) {
                moduleFiles[module.name] = files
            }
        }
        file.writeText(I18n(moduleFiles).toAsciidoc())
    }
}

tasks.asciidoctor {
    dependsOn("copyModuleDependencyGraphs", "generateI18nValuesPage")
    // since the sources included in the docs may have changed, there"s definitely
    // a more gradle like way to do this, but it escapes me
    inputs.dir(file("../demos/chinook/src"))
    inputs.dir(file("../demos/employees/src"))
    inputs.dir(file("../demos/manual/src"))
    inputs.dir(file("../demos/petclinic/src"))
    inputs.dir(file("../demos/petstore/src"))
    inputs.dir(file("../demos/world/src"))
    inputs.dir(file("../framework/server/src/main"))

    setOutputDir(project.layout.buildDirectory.dir("asciidoc"))

    baseDirFollowsSourceFile()
    sources {
        include("*.adoc", "technical/**/*.adoc", "tutorials/**/*.adoc", "manual/**/*.adoc", "help/**/*.adoc")
    }
    attributes(
        mapOf(
            "codion-version" to documentationVersion,
            "revnumber" to documentationVersion, "sectnums" to 4, "sectanchors" to true, "prewrap" to false,
            "experimental" to true, "reproducible" to true, "linkcss" to true, "tabsize" to 2,
            "diagram-cachedir" to project.layout.buildDirectory.dir("asciidoc/images/diagram-cache").get().asFile.absolutePath,
            "opar" to "(",
            "cpar" to ")",
            "comma" to ",",
            "common-core" to "/is.codion.common.core",
            "common-db" to "/is.codion.common.db",
            "common-model" to "/is.codion.common.model",
            "common-rmi" to "/is.codion.common.rmi",
            "framework-db-core" to "/is.codion.framework.db.core",
            "framework-db-http" to "/is.codion.framework.db.http",
            "framework-db-local" to "/is.codion.framework.db.local",
            "framework-db-rmi" to "/is.codion.framework.db.rmi",
            "framework-domain" to "/is.codion.framework.domain",
            "framework-domain-test" to "/is.codion.framework.domain.test",
            "framework-json-domain" to "/is.codion.framework.json.domain",
            "framework-json-db" to "/is.codion.framework.json.db",
            "framework-model" to "/is.codion.framework.model",
            "framework-model-test" to "/is.codion.framework.model.test",
            "framework-server" to "/is.codion.framework.server",
            "framework-servlet" to "/is.codion.framework.servlet",
            "plugin-jasperreports" to "/is.codion.plugin.jasperreports",
            "swing-common-model" to "/is.codion.swing.common.model",
            "tools-loadtest-model" to "/is.codion.tools.loadtest.model",
            "swing-common-ui" to "/is.codion.swing.common.ui",
            "tools-loadtest-ui" to "/is.codion.tools.loadtest.ui",
            "swing-framework-model" to "/is.codion.swing.framework.model",
            "tools-generator-model" to "/is.codion.tools.genarator.model",
            "swing-framework-ui" to "/is.codion.swing.framework.ui"
        )
    )
    asciidoctorj {
        setVersion("2.5.13")
        modules {
            diagram.use()
        }
        attributes(mapOf("source-highlighter" to "prettify"))
    }
}

tasks.register("combinedJavadoc") {
    group = "documentation"
    description = "An absolute monstrosity of a workaround for combining javadocs for multiple modular sub-projects"
    val tempDir = project.layout.buildDirectory.dir("tmp").get()
    val combinedSrcDir = "${tempDir}/combinedSource"
    val outputDirectory = project.layout.buildDirectory.dir("javadoc").get()
    val optionsFile = tempDir.file("javadoc.options").asFile

    outputs.dir(outputDirectory)
    var classpath: FileCollection = project.files()
    frameworkModules().forEach { module ->
        inputs.files(module.sourceSets.main.get().java)
        module.tasks.withType<Javadoc>().forEach { javadocTask ->
            classpath += javadocTask.classpath
        }
    }
    doLast {
        //combine the framework source, with each module in a separate directory, named after the module
        frameworkModules().forEach { module ->
            ant.withGroovyBuilder {
                "copy"("todir" to "${combinedSrcDir}/is.${module.name.replace("-", ".")}") {
                    "fileset"("dir" to "${module.projectDir}/src/main/java")
                }
            }
        }
        val options = StandardJavadocDocletOptions().apply {
            sourceNames = project.objects.fileCollection().from(combinedSrcDir).asFileTree.files.map { it.absolutePath }.toList()
            modulePath = classpath.files.toList()
            addStringOption("-module-source-path", combinedSrcDir)
            destinationDirectory = file(outputDirectory)
            docTitle = "Codion Framework API $documentationVersion"
            links("https://docs.oracle.com/en/java/javase/" + properties["jdkVersion"] + "/docs/api/")
            encoding = "UTF-8"
            noTimestamp(true)
        }

        options.write(optionsFile)

        val javadocTool = javaToolchains.javadocToolFor {
            languageVersion = JavaLanguageVersion.of(properties["jdkVersion"] as String)
        }
        providers.exec {
            executable = javadocTool.get().executablePath.asFile.absolutePath
            args = listOf("@${optionsFile.absolutePath}")
        }.result.get()

        delete(combinedSrcDir)
    }
}

tasks.register("assembleDocs") {
    dependsOn("combinedJavadoc", "asciidoctor")
    group = "documentation"
    description = "Creates the javadocs and asciidocs and combines them into the documentatio directory"
    val docFolder = project.layout.buildDirectory.dir(documentationDir).get()
    doLast {
        delete(docFolder)
        copy {
            from(project.layout.buildDirectory.dir("asciidoc").get())
            into(docFolder)
        }
        copy {
            from(project.layout.buildDirectory.dir("javadoc").get())
            into(docFolder.dir("api"))
        }
        copy {
            from(project.projectDir.resolve("src/docs/utilities"))
            into(docFolder.dir("utilities"))
        }
    }
}

tasks.register<Sync>("copyToGitHubPages") {
    dependsOn("assembleDocs")
    group = "documentation"
    description = "Copies the assembled docs to the github pages project"
    from(project.layout.buildDirectory.dir(documentationDir))
    into("../../codion-pages/doc/$documentationDir")
}

tasks.register<Zip>("documentationZip") {
    dependsOn("assembleDocs")
    group = "documentation"
    description = "Creates a zip file containing the assembled documentation"
    from(project.layout.buildDirectory.dir(documentationDir))
}

fun frameworkModules(): Iterable<Project> {
    return project.parent?.subprojects?.filter { project ->
        !project.name.contains("demos") && !project.name.contains("documentation")
    } ?: emptyList()
}