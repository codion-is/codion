import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.gradle.internal.jvm.Jvm
import java.time.LocalDate
import java.time.format.DateTimeFormatter

buildscript {
    dependencies {
        classpath("org.revapi:gradle-revapi:1.8.0")
    }
}

plugins {
    id("org.sonarqube") version "6.1.0.5360"
    id("com.github.ben-manes.versions") version "0.52.0"
    id("com.vanniktech.dependency.graph.generator") version "0.8.0"
    id("com.diffplug.spotless") version "7.0.3"
    id("org.gradlex.extra-java-module-info") version "1.12"
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("io.github.f-cramer.jasperreports") version "0.0.4"
}

nexusPublishing {
    packageGroup = "is.codion"
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
        }
    }
}

configure(frameworkModules()) {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "com.github.ben-manes.versions")
    apply(plugin = "project-report")
    apply(plugin = "com.vanniktech.dependency.graph.generator")
    apply(plugin = "signing")
//    apply(plugin = "com.palantir.revapi")

    tasks.clean {
        doLast {
            //clean intellij output dir as well
            file("out").delete()
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        withJavadocJar()
        withSourcesJar()
    }

    tasks.withType<Javadoc>().configureEach {
        val docletOptions = options as StandardJavadocDocletOptions
        docletOptions.links(
            "https://docs.oracle.com/en/java/javase/" + properties["jdkVersion"] + "/docs/api/",
            "https://jspecify.dev/docs/api/"
        )
        docletOptions.encoding = "UTF-8"
    }

    tasks.withType<Jar>().configureEach {
        manifest {
            attributes["Sealed"] = "true"
            attributes["Specification-Title"] = project.name
            attributes["Specification-Version"] = project.version
            attributes["Specification-Vendor"] = "Codion"
            attributes["Implementation-Title"] = project.name
            attributes["Implementation-Version"] = project.version
            attributes["Implementation-Vendor"] = "Codion"
            attributes["Implementation-Vendor-Id"] = "is.codion"
            attributes["Implementation-URL"] = "https://codion.is"
            attributes["Build-Jdk"] = Jvm.current()
            attributes["Built-By"] = System.getProperty("user.name")
            attributes["Build-Date"] = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDate.now())
        }
    }

    if (hasPublicationProperties()) {
        configure<PublishingExtension> {
            publications {
                create<MavenPublication>("mavenJava") {
                    groupId = "is.codion"
                    from(components["java"])
                    pom {
                        name = "is.codion:" + project.name
                        description = "Codion Application Framework"
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
                }
            }
            repositories {
                maven {
                    credentials {
                        username = properties["artifactoryUsername"].toString()
                        password = properties["artifactoryPassword"].toString()
                    }
                    url = if (project.version.toString().endsWith("-SNAPSHOT")) {
                        uri(properties["artifactorySnapshotUrl"].toString())
                    } else {
                        uri(properties["artifactoryReleaseUrl"].toString())
                    }
                }.isAllowInsecureProtocol = true
            }
        }

        configure<SigningExtension> {
            sign(project.extensions.getByType<PublishingExtension>().publications["mavenJava"])
        }
    }

    if (hasSonarqubeProperties()) {
        sonar {
            System.setProperty("sonar.projectVersion", (project.version as String).replace("-SNAPSHOT", ""))
            System.setProperty("sonar.java.source", properties["jdkVersion"].toString())
            System.setProperty("sonar.sourceEncoding", "UTF-8")
            System.setProperty("sonar.exclusions", "**/*TestDomain.java")
            System.setProperty(
                "sonar.coverage.exclusions",
                "**/is/codion/framework/model/test/**," +
                        "**/is/codion/framework/domain/entity/test/**," +
                        "**/is/codion/swing/framework/ui/test/**"
            )
        }
    }

    tasks.named("test") {
        dependsOn(tasks.named("createServerKeystore"))
        finalizedBy(tasks.named("jacocoTestReport"))
    }
}

configure(subprojects) {
    apply(plugin = "java")
    apply(plugin = "jacoco")
    apply(plugin = "com.diffplug.spotless")

    spotless {
        java {
            licenseHeaderFile(rootProject.file("documentation/src/misc/license_header")).yearSeparator(" - ")
        }
        format("javaMisc") {
            target("src/**/package-info.java", "src/**/module-info.java")
            licenseHeaderFile("${rootDir}/documentation/src/misc/license_header", "\\/\\*\\*").yearSeparator(" - ")
        }
    }

    testing {
        suites {
            val test by getting(JvmTestSuite::class) {
                useJUnitJupiter()
                targets {
                    all {
                        testTask.configure {
                            systemProperty("codion.db.url", "jdbc:h2:mem:h2db")
                            systemProperty("codion.db.initScripts", "src/test/sql/create_h2_db.sql")
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

        val keystoreDir = "${rootDir}/framework/server/src/main/config/"
        val keystore = keystoreDir + "keystore.jks"
        val truststore = keystoreDir + "truststore.jks"
        val certificate = keystoreDir + "certificate.cer"
        val keyToolExecutable = System.getProperty("java.home") + "/bin/keytool"

        onlyIf { !file(keystore).exists() }

        doLast {
            providers.exec {
                executable = keyToolExecutable
                args = listOf(
                    "-genkeypair", "-keyalg", "RSA", "-keystore", keystore, "-storepass", "crappypass",
                    "-keypass", "crappypass", "-dname", "CN=Dummy, OU=dummy, O=dummy.org, C=DU", "-alias", "Alias",
                    "-storetype", "pkcs12", "-ext", "SAN=dns:localhost"
                )
            }.result.get()
            providers.exec {
                executable = keyToolExecutable
                args = listOf(
                    "-exportcert", "-keystore", keystore, "-storepass", "crappypass",
                    "-alias", "Alias", "-rfc", "-file", certificate
                )
            }.result.get()
            providers.exec {
                executable = keyToolExecutable
                args = listOf(
                    "-import", "-alias", "Alias", "-storepass", "changeit", "-file", certificate,
                    "-keystore", truststore, "-noprompt", "-storetype", "pkcs12"
                )
            }.result.get()
            delete(certificate)
        }
    }
}

tasks.register("tagRelease") {
    group = "other"
    description = "Tags the current version as a release"

    doLast {
        if (project.version.toString().contains("SNAPSHOT")) {
            throw GradleException("Thou shalt not tag a snapshot release")
        }
        val tagName = "v" + project.version
        providers.exec { commandLine("git", "push", "dev") }.result.get()
        providers.exec { commandLine("git", "push", "origin") }.result.get()
        providers.exec { commandLine("git", "tag", "-a", tagName, "-m", "$tagName release") }.result.get()
        providers.exec { commandLine("git", "push", "dev", tagName) }.result.get()
        providers.exec { commandLine("git", "push", "origin", tagName) }.result.get()
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

fun hasPublicationProperties(): Boolean {
    return project.hasProperty("sonatypeUsername") &&
            project.hasProperty("sonatypePassword") &&
            project.hasProperty("signing.keyId") &&
            project.hasProperty("signing.password") &&
            project.hasProperty("signing.secretKeyRingFile")
}

fun hasSonarqubeProperties(): Boolean {
    return project.hasProperty("systemProp.sonar.host.url") &&
            project.hasProperty("systemProp.sonar.login") &&
            project.hasProperty("systemProp.sonar.password")
}

fun frameworkModules(): Iterable<Project> {
    return subprojects.filter { project ->
        !project.name.startsWith("demo") && project.name != "documentation"
    }
}