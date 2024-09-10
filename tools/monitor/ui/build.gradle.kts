dependencies {
    implementation(project(":codion-swing-common-ui"))
    implementation(project(":codion-tools-monitor-model"))

    implementation(libs.flatlaf.intellij.themes)
    implementation(libs.slf4j.api)

    implementation(libs.jfreechart)

    runtimeOnly(project(":codion-plugin-logback-proxy"))

    testRuntimeOnly(project(":codion-framework-db-rmi"))
    testRuntimeOnly(project(":codion-plugin-hikari-pool"))
    testRuntimeOnly(project(":codion-dbms-h2"))
    testRuntimeOnly(libs.h2)
}

tasks.register<JavaExec>("runServerMonitor") {
    group = "application"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("is.codion.tools.monitor.ui.EntityServerMonitorPanel")
    maxHeapSize = "512m"
    systemProperties = mapOf(
        "codion.server.hostname" to properties["serverHostName"],
        "codion.server.admin.user" to "scott:tiger",
        "codion.client.trustStore" to "../../../framework/server/src/main/config/truststore.jks",
        "logback.configurationFile" to "src/config/logback.xml"
    )
}