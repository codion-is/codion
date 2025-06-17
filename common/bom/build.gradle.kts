plugins {
    `java-platform`
    `maven-publish`
    signing
}

description = "Codion Common BOM - UI components, tools, and plugins without framework dependencies"

dependencies.constraints {
    api("is.codion:codion-common-core:${project.version}")
    api("is.codion:codion-common-db:${project.version}")
    api("is.codion:codion-common-i18n:${project.version}")
    api("is.codion:codion-common-model:${project.version}")
    api("is.codion:codion-common-rmi:${project.version}")

    api("is.codion:codion-swing-common-model:${project.version}")
    api("is.codion:codion-swing-common-ui:${project.version}")

    api("is.codion:codion-tools-loadtest-core:${project.version}")
    api("is.codion:codion-tools-loadtest-model:${project.version}")
    api("is.codion:codion-tools-loadtest-ui:${project.version}")

    api("is.codion:codion-dbms-h2:${project.version}")
    api("is.codion:codion-dbms-postgresql:${project.version}")
    api("is.codion:codion-dbms-oracle:${project.version}")
    api("is.codion:codion-dbms-mysql:${project.version}")
    api("is.codion:codion-dbms-mariadb:${project.version}")
    api("is.codion:codion-dbms-sqlite:${project.version}")
    api("is.codion:codion-dbms-sqlserver:${project.version}")
    api("is.codion:codion-dbms-db2:${project.version}")
    api("is.codion:codion-dbms-derby:${project.version}")
    api("is.codion:codion-dbms-hsqldb:${project.version}")

    api("is.codion:codion-plugin-flatlaf:${project.version}")
    api("is.codion:codion-plugin-flatlaf-intellij-themes:${project.version}")
    api("is.codion:codion-plugin-imagepanel:${project.version}")
    api("is.codion:codion-plugin-jasperreports:${project.version}")
    api("is.codion:codion-plugin-swing-mcp:${project.version}")
    api("is.codion:codion-plugin-jul-proxy:${project.version}")
    api("is.codion:codion-plugin-log4j-proxy:${project.version}")
    api("is.codion:codion-plugin-logback-proxy:${project.version}")
}

publishing {
    publications {
        create<MavenPublication>("bom") {
            groupId = "is.codion"
            from(components["javaPlatform"])
            pom {
                name = "Codion Common BOM"
                description = "Bill of Materials for Codion common modules - UI components, tools, and plugins without framework dependencies"
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