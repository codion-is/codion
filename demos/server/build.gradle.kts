plugins {
    application
}

dependencies {
    runtimeOnly(project(":codion-framework-server"))
    runtimeOnly(project(":codion-framework-servlet"))
    runtimeOnly(project(":codion-framework-i18n"))
    runtimeOnly (project(":codion-plugin-jasperreports")) {
        exclude(group = "org.apache.xmlgraphics")
    }
    runtimeOnly(project(":codion-plugin-hikari-pool"))
    runtimeOnly(project(":codion-plugin-logback-proxy"))

    runtimeOnly(project(path = ":demo-employees", configuration = "domain"))
    runtimeOnly(project(path = ":demo-chinook", configuration = "domain"))
    runtimeOnly(project(path = ":demo-petclinic", configuration = "domain"))
    runtimeOnly(project(path = ":demo-petstore", configuration = "domain"))
    runtimeOnly(project(path = ":demo-world", configuration = "domain"))

    runtimeOnly(project(":codion-dbms-h2"))
    runtimeOnly(libs.h2)
}

sonarqube {
    isSkipProject = true
}

tasks.withType<JavaExec>().configureEach {
    dependsOn(tasks.named("createServerKeystore"))
}

application {
    //no mainModule, since we dont want to run this on the module path, due to non-modular demo domain model dependencies
    mainClass = "is.codion.framework.server.EntityServer"
    applicationDefaultJvmArgs = listOf(
        "-Xmx512m",
        "-Dcodion.db.url=jdbc:h2:mem:h2db",
        "-Dcodion.db.initScripts=../employees/src/main/sql/create_schema.sql,../chinook/src/main/sql/create_schema.sql,../petclinic/src/main/sql/create_schema.sql,../petstore/src/main/sql/create_schema.sql,../world/src/main/sql/create_schema.sql",
        "-Dcodion.db.countQueries=true",
        "-Dcodion.server.connectionPoolUsers=scott:tiger",
        "-Dcodion.server.port=2222",
        "-Dcodion.server.admin.port=2223",
        "-Dcodion.server.admin.user=scott:tiger",
        "-Dcodion.server.http.secure=false",
        "-Dcodion.server.pooling.poolFactoryClass=is.codion.plugin.hikari.pool.HikariConnectionPoolProvider",
        "-Dcodion.server.auxiliaryServerFactoryClassNames=is.codion.framework.servlet.EntityServiceFactory",
        "-Dcodion.server.objectInputFilterFactoryClassName=is.codion.common.rmi.server.SerializationFilterFactory",
        "-Dcodion.server.serialization.filter.patternFile=src/main/config/serialization-whitelist.txt",
        "-Dcodion.server.clientLogging=false",
        "-Djavax.net.ssl.keyStore=../../framework/server/src/main/config/keystore.jks",
        "-Djavax.net.ssl.keyStorePassword=crappypass",
        "-Djava.rmi.server.hostname=" + properties["serverHostName"],
        "-Dlogback.configurationFile=src/main/config/logback.xml"
    )
}