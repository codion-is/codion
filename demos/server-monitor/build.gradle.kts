plugins {
    application
}

dependencies {
    runtimeOnly(project(":codion-tools-monitor-ui"))
}

sonarqube {
    isSkipProject = true
}

application {
    mainModule = "is.codion.tools.monitor.ui"
    mainClass = "is.codion.tools.monitor.ui.EntityServerMonitorPanel"
    applicationDefaultJvmArgs = listOf(
        "-Xmx512m",
        "-Dcodion.server.hostname=" + properties["serverHostName"],
        "-Dcodion.server.admin.user=scott:tiger",
        "-Dcodion.client.trustStore=../../framework/server/src/main/config/truststore.jks",
        "-Dlogback.configurationFile=src/config/logback.xml"
    )
}