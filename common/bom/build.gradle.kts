dependencies.constraints {
    api(project(":codion-common-core"))
    api(project(":codion-common-db"))
    api(project(":codion-common-i18n"))
    api(project(":codion-common-model"))
    api(project(":codion-common-rmi"))

    api(project(":codion-swing-common-model"))
    api(project(":codion-swing-common-ui"))

    api(project(":codion-tools-loadtest-core"))
    api(project(":codion-tools-loadtest-model"))
    api(project(":codion-tools-loadtest-ui"))

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

    api(project(":codion-plugin-flatlaf"))
    api(project(":codion-plugin-flatlaf-intellij-themes"))
    api(project(":codion-plugin-imagepanel"))
    api(project(":codion-plugin-jasperreports"))
    api(project(":codion-plugin-swing-mcp"))
    api(project(":codion-plugin-jul-proxy"))
    api(project(":codion-plugin-log4j-proxy"))
    api(project(":codion-plugin-logback-proxy"))
}