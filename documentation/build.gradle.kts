import java.util.*

plugins {
    id("org.asciidoctor.jvm.convert") version "4.0.5"
}

val documentationVersion = project.version.toString().replace("-SNAPSHOT", "")

tasks.register<Copy>("generateModuleDependencyGraphs") {
    group = "documentation"
    description = "Copies the module dependency graphs to the asciidoc images folder"

    destinationDir = project.layout.buildDirectory.dir("asciidoc/images/modules").get().asFile

    frameworkModules().forEach { module ->
        dependsOn(module.tasks.named("generateDependencyGraph"))

        val graphFile = module.file("build/reports/dependency-graph/dependency-graph.svg")
        val relativePath = module.projectDir.relativeTo(rootDir).path

        from(graphFile) {
            into("$relativePath/build/reports/dependency-graph")
        }
    }
}

tasks.register("generateI18nValuesPage") {
    group = "documentation"
    description = "Generates the i18n asciidoc page"

    inputs.files(frameworkModules().filter { module ->
        module.plugins.hasPlugin("java")
    }.map { module ->
        module.sourceSets.main.get().resources.matching {
            include("**/*.properties")
        }
    })
    outputs.file(file("src/docs/asciidoc/technical/i18n-values.adoc"))
    outputs.cacheIf { true }

    doLast {
        val outputFile = file("src/docs/asciidoc/technical/i18n-values.adoc")
        val moduleFiles = mutableMapOf<String, List<File>>()

        frameworkModules().filter { module ->
            module.plugins.hasPlugin("java")
        }.forEach { module ->
            val files = module.sourceSets.main.get().resources.matching {
                include("**/*.properties")
            }.files.toList().sortedBy { it.path }
            if (files.isNotEmpty()) {
                moduleFiles[module.name] = files
            }
        }

        outputFile.writeText(generateI18nAsciidoc(moduleFiles))
    }
}

tasks.asciidoctor {
    dependsOn("generateModuleDependencyGraphs", "generateI18nValuesPage")
    rootProject.subprojects.filter { it.name.startsWith("demo-") }.forEach { demo ->
        inputs.files(demo.sourceSets.main.get().allSource)
        inputs.files(demo.sourceSets.test.get().allSource)
    }

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
            "diagram-cachedir" to project.layout.buildDirectory.dir("asciidoc/images/diagram-cache")
                .get().asFile.absolutePath,
            "common-db" to "/is.codion.common.db",
            "common-model" to "/is.codion.common.model",
            "common-reactive" to "/is.codion.common.reactive",
            "common-rmi" to "/is.codion.common.rmi",
            "common-utilities" to "/is.codion.common.utilities",
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
        setVersion("3.0.0")
        modules {
            diagram.use()
        }
        attributes(mapOf("source-highlighter" to "prettify"))
    }
}

tasks.register("combinedJavadoc") {
    group = "documentation"
    description = "Generates combined Javadocs for all framework modules"

    // Define directories
    val tempDir = layout.buildDirectory.dir("tmp/javadoc")
    val combinedSourceDir = tempDir.map { it.dir("combined-source") }
    val outputDir = layout.buildDirectory.dir("javadoc")

    // Track inputs and outputs
    frameworkModules().filter { module ->
        module.plugins.hasPlugin("java")
    }.forEach { module ->
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

        frameworkModules().filter { module ->
            module.plugins.hasPlugin("java")
        }.forEach { module ->
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
            isIgnoreExitValue = true
        }

        val exitValue = execResult.result.get().exitValue
        if (exitValue != 0) {
            println("STDOUT:")
            println(execResult.standardOutput.asText.get())
            println("STDERR:")
            println(execResult.standardError.asText.get())
            throw GradleException("Javadoc failed with exit code $exitValue")
        }

        // Clean up temporary directory
        delete(tempDir)
    }
}

tasks.register("checkI18nDocs") {
    group = "verification"
    description = "Verifies that i18n documentation is up to date"

    dependsOn("generateI18nValuesPage")

    doLast {
        val generatedFile = file("src/docs/asciidoc/technical/i18n-values.adoc")
        val tempFile = layout.buildDirectory.file("tmp/i18n-values-check.adoc").get().asFile

        // Save current content
        if (generatedFile.exists()) {
            tempFile.parentFile.mkdirs()
            generatedFile.copyTo(tempFile, overwrite = true)

            // Run generation
            tasks.named("generateI18nValuesPage")
                .get().actions.forEach { it.execute(tasks.named("generateI18nValuesPage").get()) }

            // Compare
            if (tempFile.readText() != generatedFile.readText()) {
                throw GradleException("i18n documentation is out of date. Run './gradlew :documentation:generateI18nValuesPage' to update it.")
            }

            tempFile.delete()
        }
    }
}

tasks.register("assembleDocs") {
    dependsOn("combinedJavadoc", "asciidoctor", "assembleTutorials")
    group = "documentation"
    description = "Creates the javadocs and asciidocs and combines them into the documentation directory"
    val docFolder = project.layout.buildDirectory.dir(documentationVersion).get()
    doLast {
        delete(docFolder)
        copy {
            from(project.layout.buildDirectory.dir("asciidoc").get())
            into(docFolder)
        }
        copy {
            from(project.layout.buildDirectory.dir("tutorials").get())
            into(docFolder.dir("tutorials"))
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

tasks.register("upgradeDemoApps") {
    group = "documentation"
    description = "Updates the Codion version in all demo project libs.versions.toml files"

    doLast {
        val demoProjects = listOf("sdkboy", "llemmy", "petclinic", "world", "chinook")
        val versionPattern = Regex("""codion\s*=\s*"[^"]+"""")
        val newVersionLine = """codion = "$documentationVersion""""

        demoProjects.forEach { project ->
            val tomlFile = file("../../$project/gradle/libs.versions.toml")
            if (tomlFile.exists()) {
                val content = tomlFile.readText()
                val updatedContent = content.replace(versionPattern, newVersionLine)
                if (content != updatedContent) {
                    tomlFile.writeText(updatedContent)
                    println("Updated $project to Codion $documentationVersion")
                } else {
                    println("$project already at Codion $documentationVersion")
                }
            } else {
                println("Warning: $tomlFile not found")
            }
        }
    }
}

tasks.register("assembleTutorials") {
    group = "documentation"
    description = "Assembles tutorials from demo projects"

    doLast {
        val docFolder = project.layout.buildDirectory.dir("tutorials").get()
        val demoProjects = listOf(
            Triple("sdkboy", "asciidoctor", "build/docs/asciidoc"),
            Triple("llemmy", "asciidoctor", "llemmy/build/docs/asciidoc"),
            Triple("petclinic", "asciidoctor", "build/docs/asciidoc"),
            Triple("world", "documentation:asciidoctor", "documentation/build/docs/asciidoc"),
            Triple("chinook", "documentation:asciidoctor", "documentation/build/docs/asciidoc")
        )

        demoProjects.forEach { (project, task, outputPath) ->
            println("Processing $project...")

            val projectDir = file("../../$project")

            val exec = providers.exec {
                workingDir(projectDir)
                commandLine("./gradlew", task)
                environment("JAVA_HOME", System.getenv("JAVA_HOME") ?: System.getProperty("java.home"))
            }
            exec.result.get()
            println(exec.standardOutput.asText.get())

            copy {
                from("../../$project/$outputPath")
                into(docFolder.dir(project))
            }
        }
    }
}

tasks.register<Sync>("copyToGitHubPages") {
    dependsOn("linkcheck")
    group = "documentation"
    description = "Copies the assembled docs to the github pages project"
    from(project.layout.buildDirectory.dir(documentationVersion))
    into("../../codion-pages/doc/$documentationVersion")
}

tasks.register<Sync>("deploy") {
    dependsOn("clean", "copyToGitHubPages")
    group = "documentation"
    description = "Clean, assembles and copies the docs to the github pages project"
}

tasks.register("linkcheck") {
    dependsOn("assembleDocs")
    group = "documentation"
    description = "Runs linkcheck on the assembled documentation"

    doLast {
        val docDir = project.layout.buildDirectory.dir(documentationVersion).get().asFile
        val port = 8080

        val javaHome = System.getenv("JAVA_HOME") ?: System.getProperty("java.home")
        val javaExecutable = file("$javaHome/bin/java").absolutePath

        val serverProcess =
            ProcessBuilder(javaExecutable, "-m", "jdk.httpserver", "-p", port.toString(), "-d", docDir.absolutePath)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()

        println("Started jwebserver on port $port, serving ${docDir.absolutePath}")

        Thread.sleep(2000)

        if (!serverProcess.isAlive) {
            throw GradleException("Server process died immediately")
        }

        try {
            println("Running linkcheck...")
            val exec = providers.exec {
                commandLine("docker", "run", "--rm", "--network=host", "tennox/linkcheck", "http://localhost:$port")
                isIgnoreExitValue = true // Don't fail on warnings (exit code 1), only on errors (exit code 2)
            }
            val execResult = exec.result.get()
            println(exec.standardOutput.asText.get())
            when (execResult.exitValue) {
                0 -> println("Linkcheck completed successfully")
                1 -> println("Linkcheck completed with warnings")
                2 -> throw GradleException("Linkcheck failed with errors")
                else -> throw GradleException("Linkcheck failed with unexpected exit code: ${execResult.exitValue}")
            }
        } finally {
            serverProcess.destroy()
            println("Stopped jwebserver")
        }
    }
}

tasks.register<Exec>("serveDocs") {
    dependsOn("assembleDocs")
    group = "documentation"
    description = "Assembles and serves the docs on localhost:8080"

    val javaHome = System.getenv("JAVA_HOME") ?: System.getProperty("java.home")
    val javaExecutable = file("$javaHome/bin/java").absolutePath

    commandLine = listOf(
        javaExecutable, "-m", "jdk.httpserver", "-p", "8080", "-d",
        project.layout.buildDirectory.dir(documentationVersion).get().asFile.absolutePath
    )
}

fun frameworkModules(): Iterable<Project> {
    return project.parent?.subprojects?.filter { project ->
        !project.name.startsWith("demo") && project.name != "documentation" && !project.name.endsWith("-bom")
    } ?: emptyList()
}

fun generateI18nAsciidoc(moduleFiles: Map<String, List<File>>): String {
    val localePattern = Regex(".*_[a-z]{2}_[A-Z]{2}\\.properties")

    data class Resource(
        val owner: String,
        val locales: List<String>,
        val localeFiles: Map<String, File>,
        val localeProperties: Map<String, Properties>
    )

    fun parseLocale(file: File): String {
        val fileName = file.name
        val match = Regex("_([a-z]{2}_[A-Z]{2})\\.properties$").find(fileName)
        return match?.groupValues?.get(1) ?: "default"
    }

    fun loadProperties(file: File): Properties {
        return Properties().apply {
            file.inputStream().use { load(it) }
        }
    }

    fun cleanResourcePath(file: File): String {
        val path = file.path.replace("\\", "/")
        val resourcesIndex = path.indexOf("src/main/resources/")
        return if (resourcesIndex >= 0) {
            path.substring(resourcesIndex + 19)
        } else {
            file.name
        }
    }

    fun extractResourceOwner(file: File): String {
        val cleanPath = cleanResourcePath(file)
        return when {
            localePattern.matches(cleanPath) -> cleanPath.substring(0, cleanPath.length - 17)
            else -> cleanPath.substring(0, cleanPath.length - 11)
        }
    }

    val moduleResources = moduleFiles.map { (module, files) ->
        val resourceMap = files.groupBy { extractResourceOwner(it) }
            .map { (owner, ownerFiles) ->
                val localeFiles = ownerFiles.associateBy { parseLocale(it) }
                val localeProperties = localeFiles.mapValues { (_, file) -> loadProperties(file) }
                Resource(owner, localeFiles.keys.sorted(), localeFiles, localeProperties)
            }
        module to resourceMap
    }

    return buildString {
        appendLine("= Values")
        appendLine()
        appendLine("Overview of the available i18n properties files and their keys and values.")
        appendLine()

        moduleResources.forEach { (module, resources) ->
            appendLine("== $module")
            appendLine()

            resources.forEach { resource ->
                appendLine("=== ${resource.owner}.java")
                appendLine()
                appendLine("[source]")
                appendLine("----")
                resource.localeFiles.values.forEach { file ->
                    appendLine(cleanResourcePath(file))
                }
                appendLine("----")
                appendLine()

                // Table header
                append("[cols=\"")
                append((0..resource.locales.size).joinToString(",") { "1" })
                appendLine("\"]")
                appendLine("|===")
                append("|key")
                resource.locales.forEach { locale ->
                    append("|$locale")
                }
                appendLine()
                appendLine()

                // Table rows
                val defaultProperties = resource.localeProperties["default"] ?: Properties()
                val propertyNames = defaultProperties.stringPropertyNames().sorted()

                for (propertyName in propertyNames) {
                    append("|$propertyName")
                    for (locale in resource.locales) {
                        val value = resource.localeProperties[locale]?.getProperty(propertyName) ?: ""
                        append("|${value.replace("|", "\\|")}")
                    }
                    appendLine()
                }

                appendLine("|===")
                appendLine()
            }
        }
    }.trim()
}