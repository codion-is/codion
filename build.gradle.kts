import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.vanniktech.maven.publish.*
import org.gradle.kotlin.dsl.support.serviceOf

plugins {
    id("org.sonarqube") version "7.3.1.8318"
    id("com.vanniktech.dependency.graph.generator") version "0.8.0"
    id("com.diffplug.spotless") version "8.9.0"
    id("org.gradlex.extra-java-module-info") version "1.14.2"
    id("com.vanniktech.maven.publish") version "0.37.0" apply false
    id("io.github.f-cramer.jasperreports") version "0.0.4"
}

configure(frameworkModules()) {
    apply(plugin = "java-library")
    apply(plugin = "com.vanniktech.maven.publish")
    apply(plugin = "com.vanniktech.dependency.graph.generator")

    tasks.clean {
        doLast {
            //clean intellij output dir as well
            file("out").delete()
        }
    }

    tasks.withType<Javadoc>().configureEach {
        val docletOptions = options as StandardJavadocDocletOptions
        docletOptions.links(
            "https://docs.oracle.com/en/java/javase/" + project.findProperty("jdkVersion") + "/docs/api/",
            "https://jspecify.dev/docs/api/"
        )
        docletOptions.encoding = "UTF-8"
    }

    tasks.withType<Jar>().configureEach {
        manifest {
            attributes["Implementation-Title"] = project.name
            attributes["Implementation-Version"] = project.version
            attributes["Implementation-Vendor"] = "Codion"
            attributes["Implementation-URL"] = "https://codion.is"
            attributes["Sealed"] = "true"
        }
    }

    configure<MavenPublishBaseExtension> {
        publishToMavenCentral()
        signAllPublications()
        configure(JavaLibrary(javadocJar = JavadocJar.Javadoc(), sourcesJar = SourcesJar.Sources()))
        coordinates("is.codion", project.name, project.version.toString())
        pom(codionPom("is.codion:" + project.name, "Codion Application Framework"))
    }

    if (hasSonarqubeProperties()) {
        sonar {
            System.setProperty("sonar.projectVersion", project.version as String)
            System.setProperty("sonar.java.source", project.findProperty("jdkVersion").toString())
            System.setProperty("sonar.sourceEncoding", "UTF-8")
            System.setProperty("sonar.exclusions", "**/*TestDomain.java")
            System.setProperty("sonar.coverage.exclusions", "**/is/codion/framework/model/test/**,**/is/codion/framework/domain/test/**")
        }
    }

    tasks.named("test") {
        dependsOn(tasks.named("createServerKeystore"))
        finalizedBy(tasks.named("jacocoTestReport"))
    }
}

configure(bomModules()) {
    apply(plugin = "java-platform")
    apply(plugin = "com.vanniktech.maven.publish")

    configure<MavenPublishBaseExtension> {
        publishToMavenCentral()
        signAllPublications()
        configure(JavaPlatform())
        coordinates("is.codion", project.name, project.version.toString())
        pom(codionPom("is.codion:" + project.name, "Codion Application Framework BOM"))
    }
}

val junitVersion = libs.versions.junit.get()

configure(subprojects.filter { it.name != "codion-framework-bom" && it.name != "codion-common-bom" }) {
    apply(plugin = "java")
    apply(plugin = "jacoco")
    apply(plugin = "com.diffplug.spotless")

    spotless {
        java {
            licenseHeaderFile(rootProject.file("documentation/src/misc/license_header")).yearSeparator(" - ")
            targetExclude(
                "src/main/java/is/codion/framework/db/local/QueryFormatter.java",
                "src/main/java/is/codion/swing/common/model/component/button/NullableToggleButtonModel.java",
                "src/main/java/is/codion/swing/common/ui/component/button/NullableCheckBox.java",
                "src/main/java/is/codion/swing/common/ui/component/combobox/Completion.java",
                "src/main/java/is/codion/swing/common/ui/component/combobox/CompletionDocument.java",
                "src/main/java/is/codion/swing/common/ui/component/image/ImagePane.java"
            )
        }
        format("javaMisc") {
            target("src/**/package-info.java", "src/**/module-info.java")
            licenseHeaderFile("${rootDir}/documentation/src/misc/license_header", "\\/\\*\\*").yearSeparator(" - ")
        }
    }

    testing {
        suites {
            getByName<JvmTestSuite>("test") {
                useJUnitJupiter(junitVersion)
                targets {
                    all {
                        testTask.configure {
                            systemProperty("codion.db.url", "jdbc:h2:mem:h2db")
                            systemProperty("codion.db.initScripts", "src/test/sql/create_h2_db.sql")
                            systemProperty("codion.db.urlScopedInstance", "true")
                            systemProperty("codion.test.user", "scott:tiger")
                        }
                    }
                }
            }
        }
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.isDeprecation = true
        options.compilerArgs.addAll(listOf("--module-version", project.version.toString()))
    }

    tasks.withType<JacocoReport>().configureEach {
        reports {
            xml.required = true
            html.required = true
            csv.required = true
        }
        dependsOn(tasks.test)
    }

    tasks.register("createServerKeystore") {
        group = "other"
        description = "Creates a key and truststore pair to use when running server unit tests and demos with remote connection"

        val keystoreDir = rootProject.layout.projectDirectory.dir("framework/server/src/main/config")
        val keystore = keystoreDir.file("keystore.jks")
        val truststore = keystoreDir.file("truststore.jks")
        val certificate = keystoreDir.file("certificate.cer")
        val keyToolExecutable = System.getProperty("java.home") + "/bin/keytool"

        onlyIf { !keystore.asFile.exists() }

        val execOperations = project.serviceOf<ExecOperations>()
        doLast {
            execOperations.exec {
                executable = keyToolExecutable
                args = listOf(
                    "-genkeypair", "-keyalg", "RSA", "-keystore", keystore.asFile.absolutePath, "-storepass", "crappypass",
                    "-keypass", "crappypass", "-dname", "CN=Dummy, OU=dummy, O=dummy.org, C=DU", "-alias", "Alias",
                    "-storetype", "pkcs12", "-ext", "SAN=dns:localhost"
                )
            }
            execOperations.exec {
                executable = keyToolExecutable
                args = listOf(
                    "-exportcert", "-keystore", keystore.asFile.absolutePath, "-storepass", "crappypass",
                    "-alias", "Alias", "-rfc", "-file", certificate.asFile.absolutePath
                )
            }
            execOperations.exec {
                executable = keyToolExecutable
                args = listOf(
                    "-import", "-alias", "Alias", "-storepass", "changeit", "-file", certificate.asFile.absolutePath,
                    "-keystore", truststore.asFile.absolutePath, "-noprompt", "-storetype", "pkcs12"
                )
            }
            delete(certificate)
        }
    }
}

tasks.register("tagRelease") {
    group = "other"
    description = "Tags the current version as a release"

    val execOperations = project.serviceOf<ExecOperations>()
    doLast {
        if (project.version.toString().contains("SNAPSHOT")) {
            throw GradleException("Thou shalt not tag a snapshot release")
        }
        val tagName = "v" + project.version
        execOperations.exec { commandLine("git", "push", "dev") }
        execOperations.exec { commandLine("git", "push", "origin") }
        execOperations.exec { commandLine("git", "tag", "-a", tagName, "-m", "$tagName release") }
        execOperations.exec { commandLine("git", "push", "dev", tagName) }
        execOperations.exec { commandLine("git", "push", "origin", tagName) }
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}

fun codionPom(pomName: String, pomDescription: String) = Action<MavenPom> {
    name = pomName
    description = pomDescription
    url = "https://codion.is"
    licenses {
        license {
            name = "GPL-3.0"
            url = "https://www.gnu.org/licenses/gpl-3.0.en.html"
        }
    }
    developers {
        developer {
            id = "bjorndarri"
            name = "Björn Darri Sigurðsson"
            email = "bjorndarri@gmail.com"
        }
    }
    scm {
        connection = "scm:git:git://github.com/codion-is/codion.git"
        developerConnection = "scm:git:git://github.com/codion-is/codion.git"
        url = "https://github.com/codion-is/codion"
    }
}

fun hasSonarqubeProperties(): Boolean {
    return project.hasProperty("systemProp.sonar.host.url") &&
            project.hasProperty("systemProp.sonar.login") &&
            project.hasProperty("systemProp.sonar.password")
}

fun frameworkModules(): Iterable<Project> {
    return subprojects.filter { project ->
        !project.name.startsWith("demo") && project.name != "documentation" &&
                project.name != "codion-framework-bom" && project.name != "codion-common-bom"
    }
}

fun bomModules(): Iterable<Project> {
    return subprojects.filter { project ->
        project.name.endsWith("-bom")
    }
}