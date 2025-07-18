javaPlatform {
    allowDependencies()
}

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