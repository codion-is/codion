plugins {
    `java-platform`
    `maven-publish`
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
        create<MavenPublication>("maven") {
            from(components["javaPlatform"])
            
            pom {
                name.set("Codion Common BOM")
                description.set("Bill of Materials for Codion common modules - UI components, tools, and plugins without framework dependencies")
                url.set("https://codion.is")
                
                licenses {
                    license {
                        name.set("GPL-3.0-or-later")
                        url.set("https://www.gnu.org/licenses/gpl-3.0.html")
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
                    connection.set("scm:git:https://github.com/codion-is/codion.git")
                    developerConnection.set("scm:git:https://github.com/codion-is/codion.git")
                    url.set("https://github.com/codion-is/codion")
                }
            }
        }
    }
}