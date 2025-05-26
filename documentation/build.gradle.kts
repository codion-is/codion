plugins {
    id("org.asciidoctor.jvm.convert") version "4.0.4"
}

val documentationVersion = project.version.toString().replace("-SNAPSHOT", "")

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

    inputs.files(frameworkModules().map { module ->
        module.sourceSets.main.get().resources.matching {
            include("**/*.properties")
        }
    })
    outputs.file(file("src/docs/asciidoc/technical/i18n-values.adoc"))

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

val combinedJavadoc by tasks.registering {
    group = "documentation"
    description = "Generates combined Javadocs for all framework modules"
    
    // Define directories
    val tempDir = layout.buildDirectory.dir("tmp/javadoc")
    val combinedSourceDir = tempDir.map { it.dir("combined-source") }
    val outputDir = layout.buildDirectory.dir("javadoc")
    
    // Track inputs and outputs
    frameworkModules().forEach { module ->
        inputs.files(module.sourceSets.main.get().allJava)
        inputs.files(module.sourceSets.main.get().output)
    }
    outputs.dir(outputDir)
    
    doLast {
        // Clean and create directories
        delete(tempDir)
        mkdir(combinedSourceDir)
        
        // Prepare module source structure
        val moduleNames = mutableListOf<String>()
        val classpath = mutableSetOf<File>()
        
        frameworkModules().forEach { module ->
            val moduleName = "is.${module.name.replace("-", ".")}"
            moduleNames.add(moduleName)
            
            // Copy source files maintaining module structure
            copy {
                from(module.file("src/main/java"))
                into(combinedSourceDir.get().dir(moduleName))
            }
            
            // Collect classpath
            module.sourceSets.main.get().compileClasspath.forEach { classpath.add(it) }
            module.sourceSets.main.get().output.files.forEach { classpath.add(it) }
        }
        
        // Prepare javadoc arguments
        val javadocArgs = mutableListOf<String>().apply {
            // Output directory
            add("-d"); add(outputDir.get().asFile.absolutePath)
            
            // Module configuration
            add("--module-source-path"); add(combinedSourceDir.get().asFile.absolutePath)
            add("--add-modules"); add(moduleNames.joinToString(","))
            add("--module-path"); add(classpath.joinToString(File.pathSeparator))
            
            // Documentation options
            add("-doctitle"); add("Codion Framework API $documentationVersion")
            add("-windowtitle"); add("Codion Framework API $documentationVersion")
            add("-encoding"); add("UTF-8")
            add("-charset"); add("UTF-8")
            add("-author")
            add("-use")
            add("-notimestamp")
            add("-Xdoclint:none")
            add("-quiet")
            
            // Links to external documentation
            add("-link"); add("https://docs.oracle.com/en/java/javase/${properties["jdkVersion"]}/docs/api/")
            add("-link"); add("https://jspecify.dev/docs/api/")
            
            // Add all source files
            fileTree(combinedSourceDir).matching {
                include("**/*.java")
            }.files.forEach { sourceFile ->
                add(sourceFile.absolutePath)
            }
        }
        
        // Execute javadoc
        val javadocTool = javaToolchains.javadocToolFor {
            languageVersion = JavaLanguageVersion.of(21)
        }
        
        val execResult = providers.exec {
            executable = javadocTool.get().executablePath.asFile.absolutePath
            args = javadocArgs
        }
        
        execResult.result.get()
        
        // Clean up temporary directory
        delete(tempDir)
    }
}

tasks.register("assembleDocs") {
    dependsOn("combinedJavadoc", "asciidoctor")
    group = "documentation"
    description = "Creates the javadocs and asciidocs and combines them into the documentatio directory"
    val docFolder = project.layout.buildDirectory.dir(documentationVersion).get()
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
    from(project.layout.buildDirectory.dir(documentationVersion))
    into("../../codion-pages/doc/$documentationVersion")
}

tasks.register<Zip>("documentationZip") {
    dependsOn("assembleDocs")
    group = "documentation"
    description = "Creates a zip file containing the assembled documentation"
    from(project.layout.buildDirectory.dir(documentationVersion))
}

fun frameworkModules(): Iterable<Project> {
    return project.parent?.subprojects?.filter { project ->
        !project.name.startsWith("demo") && !project.name.equals("documentation")
    } ?: emptyList()
}