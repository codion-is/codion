plugins {
    `java-platform`
    `maven-publish`
    signing
}

javaPlatform {
    allowDependencies()
}

description = "Codion Framework BOM - Bill of Materials for the complete Codion framework including common modules"

dependencies {
    api(platform(project(":codion-common-bom")))

    constraints {
        api(project(":codion-framework-domain"))
        api(project(":codion-framework-domain-db"))
        api(project(":codion-framework-domain-test"))
        api(project(":codion-framework-db-core"))
        api(project(":codion-framework-db-local"))
        api(project(":codion-framework-db-rmi"))
        api(project(":codion-framework-db-http"))
        api(project(":codion-framework-json-domain"))
        api(project(":codion-framework-json-db"))
        api(project(":codion-framework-model"))
        api(project(":codion-framework-model-test"))
        api(project(":codion-framework-server"))
        api(project(":codion-framework-servlet"))
        api(project(":codion-framework-lambda"))
        api(project(":codion-framework-i18n"))

        api(project(":codion-swing-common-model"))
        api(project(":codion-swing-common-ui"))
        api(project(":codion-swing-framework-model"))
        api(project(":codion-swing-framework-ui"))

        api(project(":codion-plugin-hikari-pool"))
        api(project(":codion-plugin-tomcat-pool"))

        api(project(":codion-tools-generator-domain"))
        api(project(":codion-tools-generator-model"))
        api(project(":codion-tools-generator-ui"))
        api(project(":codion-tools-monitor-model"))
        api(project(":codion-tools-monitor-ui"))
    }
}

apply(plugin = "maven-publish")

publishing {
    publications {
        create<MavenPublication>("bom") {
            groupId = "is.codion"
            from(components["javaPlatform"])
            pom {
                name = "Codion Framework BOM"
                description = "Bill of Materials for the complete Codion framework including common modules"
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
}

signing {
    sign(publishing.publications["bom"])
}