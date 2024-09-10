plugins {
    id("org.gradlex.extra-java-module-info")
}

val serverRuntime: Configuration by configurations.creating {
    extendsFrom(configurations.runtimeClasspath.get())
}

dependencies {
    api(project(":codion-common-rmi"))
    api(project(":codion-framework-db-core"))

    implementation(project(":codion-framework-db-local"))
    implementation(project(":codion-framework-db-rmi"))

    implementation(libs.slf4j.api)

    testRuntimeOnly(project(":codion-plugin-hikari-pool"))
    testRuntimeOnly(project(":codion-dbms-h2"))
    testRuntimeOnly(libs.h2)

    serverRuntime(project(":codion-framework-i18n"))
    serverRuntime(project(":codion-framework-server"))
    serverRuntime(project(":codion-framework-servlet"))
    serverRuntime (project(":codion-plugin-jasperreports")) {
        exclude(group = "org.apache.xmlgraphics")
    }
    serverRuntime(project(":codion-plugin-hikari-pool"))
    serverRuntime(project(":codion-plugin-logback-proxy"))

    serverRuntime(project(path = ":codion-demos-employees", configuration = "domain"))
    serverRuntime(project(path = ":codion-demos-chinook", configuration = "domain"))
    serverRuntime(project(path = ":codion-demos-petclinic", configuration = "domain"))
    serverRuntime(project(path = ":codion-demos-petstore", configuration = "domain"))
    serverRuntime(project(path = ":codion-demos-world", configuration = "domain"))

    serverRuntime(project(":codion-dbms-h2"))
    serverRuntime(libs.h2)
}

apply(from = "../../plugins/jasperreports/extra-module-info-jasperreports.gradle")

tasks.register<JavaExec>("runServer") {
    dependsOn(tasks.named("createServerKeystore"))
    group = "application"
    classpath = serverRuntime
    mainClass.set("is.codion.framework.server.EntityServer")
    maxHeapSize = "256m"
    systemProperties = mapOf(
            "codion.db.url"                                   to "jdbc:h2:mem:h2db",
            "codion.db.initScripts"                           to "../../demos/employees/src/main/sql/create_schema.sql,../../demos/chinook/src/main/sql/create_schema.sql,../../demos/petclinic/src/main/sql/create_schema.sql,../../demos/petstore/src/main/sql/create_schema.sql,../../demos/world/src/main/sql/create_schema.sql",
            "codion.db.countQueries"                          to "true",
            "codion.server.connectionPoolUsers"               to "scott:tiger",
            "codion.server.port"                              to "2222",
            "codion.server.admin.port"                        to "2223",
            "codion.server.admin.user"                        to "scott:tiger",
            "codion.server.http.secure"                       to "false",
            "codion.server.pooling.poolFactoryClass"          to "is.codion.plugin.hikari.pool.HikariConnectionPoolProvider",
            "codion.server.auxiliaryServerFactoryClassNames"  to "is.codion.framework.servlet.EntityServiceFactory",
            "codion.server.objectInputFilterFactoryClassName" to "is.codion.common.rmi.server.WhitelistInputFilterFactory",
            "codion.server.serializationFilterWhitelist"      to "src/main/config/serialization-whitelist.txt",
            "codion.server.serializationFilterDryRun"         to "false",
            "codion.server.clientLogging"                     to "false",
            "javax.net.ssl.keyStore"                          to "src/main/config/keystore.jks",
            "javax.net.ssl.keyStorePassword"                  to "crappypass",
            "java.rmi.server.hostname"                        to properties["serverHostName"],
            "logback.configurationFile"                       to "src/main/config/logback.xml"
    )
}