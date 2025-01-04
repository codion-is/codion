plugins {
    application
}

dependencies {
    implementation(project(":codion-swing-common-ui"))
    implementation(project(":codion-tools-monitor-model"))
    implementation(project(":codion-plugin-intellij-themes"))

    implementation(libs.slf4j.api)

    implementation(libs.jfreechart)

    runtimeOnly(project(":codion-plugin-logback-proxy"))

    testRuntimeOnly(project(":codion-framework-db-rmi"))
    testRuntimeOnly(project(":codion-plugin-hikari-pool"))
    testRuntimeOnly(project(":codion-dbms-h2"))
    testRuntimeOnly(libs.h2)
}

application {
    mainClass = "is.codion.tools.monitor.ui.EntityServerMonitorPanel"
    applicationDefaultJvmArgs = listOf(
        "-Xmx512m",
        "-Dcodion.server.hostname=" + properties["serverHostName"],
        "-Dcodion.server.admin.user=scott:tiger",
        "-Dcodion.client.trustStore=../../../framework/server/src/main/config/truststore.jks",
        "-Dlogback.configurationFile=src/config/logback.xml"
    )
}