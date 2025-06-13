plugins {
    `java-platform`
    `maven-publish`
}

description = "Codion Bill of Materials (BOM) - manages versions for all Codion modules"

dependencies {
    constraints {
        // Common modules
        api(project(":codion-common-core"))
        api(project(":codion-common-db"))
        api(project(":codion-common-model"))
        api(project(":codion-common-rmi"))
        api(project(":codion-common-i18n"))

        // Framework core modules
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
        api(project(":codion-framework-i18n"))

        // Swing modules
        api(project(":codion-swing-common-model"))
        api(project(":codion-swing-common-ui"))
        api(project(":codion-swing-framework-model"))
        api(project(":codion-swing-framework-ui"))

        // Database modules
        api(project(":codion-dbms-h2"))
        api(project(":codion-dbms-postgresql"))
        api(project(":codion-dbms-oracle"))
        api(project(":codion-dbms-mysql"))
        api(project(":codion-dbms-mariadb"))
        api(project(":codion-dbms-sqlite"))
        api(project(":codion-dbms-sqlserver"))
        api(project(":codion-dbms-db2"))
        api(project(":codion-dbms-derby"))
        api(project(":codion-dbms-hsqldb"))

        // Plugin modules
        api(project(":codion-plugin-hikari-pool"))
        api(project(":codion-plugin-tomcat-pool"))
        api(project(":codion-plugin-jasperreports"))
        api(project(":codion-plugin-flatlaf"))
        api(project(":codion-plugin-flatlaf-intellij-themes"))
        api(project(":codion-plugin-jul-proxy"))
        api(project(":codion-plugin-log4j-proxy"))
        api(project(":codion-plugin-logback-proxy"))
        api(project(":codion-plugin-imagepanel"))
        api(project(":codion-plugin-swing-mcp"))

        // Tools modules
        api(project(":codion-tools-loadtest-core"))
        api(project(":codion-tools-loadtest-model"))
        api(project(":codion-tools-loadtest-ui"))
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
        create<MavenPublication>("mavenJava") {
            groupId = "is.codion"
            from(components["javaPlatform"])

            pom {
                name.set("Codion BOM")
                description.set("Bill of Materials for Codion framework - manages compatible versions of all Codion modules")
                url.set("https://codion.is")

                licenses {
                    license {
                        name.set("GPL-3.0")
                        url.set("https://www.gnu.org/licenses/gpl-3.0.en.html")
                    }
                }

                developers {
                    developer {
                        id.set("bjorndarri")
                        name.set("Björn Darri Sigurðsson")
                        email.set("bjorndarri@gmail.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/codion-is/codion.git")
                    developerConnection.set("scm:git:git://github.com/codion-is/codion.git")
                    url.set("https://github.com/codion-is/codion")
                }
            }
        }
    }
}