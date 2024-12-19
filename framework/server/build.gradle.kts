plugins {
    application
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

    serverRuntime(project(path = ":demo-employees", configuration = "domain"))
    serverRuntime(project(path = ":demo-chinook", configuration = "domain"))
    serverRuntime(project(path = ":demo-petclinic", configuration = "domain"))
    serverRuntime(project(path = ":demo-petstore", configuration = "domain"))
    serverRuntime(project(path = ":demo-world", configuration = "domain"))

    serverRuntime(project(":codion-dbms-h2"))
    serverRuntime(libs.h2)
}

apply(from = "../../plugins/jasperreports/extra-module-info-jasperreports.gradle")

tasks.withType<JavaExec>().configureEach {
    classpath = serverRuntime
    dependsOn(tasks.named("createServerKeystore"))
}

application {
    mainClass = "is.codion.framework.server.EntityServer"
    applicationDefaultJvmArgs = listOf(
        "-Xmx256m",
        "-Dcodion.db.url=jdbc:h2:mem:h2db",
        "-Dcodion.db.initScripts=../../demos/employees/src/main/sql/create_schema.sql,../../demos/chinook/src/main/sql/create_schema.sql,../../demos/petclinic/src/main/sql/create_schema.sql,../../demos/petstore/src/main/sql/create_schema.sql,../../demos/world/src/main/sql/create_schema.sql",
        "-Dcodion.db.countQueries=true",
        "-Dcodion.server.connectionPoolUsers=scott:tiger",
        "-Dcodion.server.port=2222",
        "-Dcodion.server.admin.port=2223",
        "-Dcodion.server.admin.user=scott:tiger",
        "-Dcodion.server.http.secure=false",
        "-Dcodion.server.pooling.poolFactoryClass=is.codion.plugin.hikari.pool.HikariConnectionPoolProvider",
        "-Dcodion.server.auxiliaryServerFactoryClassNames=is.codion.framework.servlet.EntityServiceFactory",
        "-Dcodion.server.objectInputFilterFactoryClassName=is.codion.common.rmi.server.WhitelistInputFilterFactory",
        "-Dcodion.server.serializationFilterWhitelist=src/main/config/serialization-whitelist.txt",
        "-Dcodion.server.serializationFilterDryRun=false",
        "-Dcodion.server.clientLogging=false",
        "-Djavax.net.ssl.keyStore=src/main/config/keystore.jks",
        "-Djavax.net.ssl.keyStorePassword=crappypass",
        "-Djava.rmi.server.hostname=" + properties["serverHostName"],
        "-Dlogback.configurationFile=src/main/config/logback.xml"
    )
}