plugins {
    id("org.gradlex.extra-java-module-info")
    id("io.github.f-cramer.jasperreports")
}

dependencies {
    implementation(project(":codion-framework-db-local"))
    implementation(project(":codion-framework-db-http"))
    implementation(project(":codion-framework-db-rmi"))
    implementation(project(":codion-tools-loadtest-ui"))
    implementation(project(":codion-swing-framework-ui"))

    implementation(project(":codion-plugin-imagepanel"))
    implementation(project(":codion-plugin-jasperreports")) {
        exclude(group = "org.apache.xmlgraphics")
    }
    implementation(libs.jfreechart)

    runtimeOnly(project(":codion-plugin-logback-proxy"))

    implementation(project(":codion-plugin-flatlaf"))
    implementation(project(":codion-plugin-flatlaf-intellij-themes"))
    implementation(libs.ikonli.foundation.pack)

    implementation(project(":codion-plugin-swing-mcp"))

    testImplementation(project(":codion-framework-domain-test"))

    jasperreportsClasspath(libs.jasperreports.jdt) {
        exclude(group = "net.sf.jasperreports")
    }

    runtimeOnly(project(":codion-dbms-h2"))
    runtimeOnly(libs.h2)
}

sonarqube {
    isSkipProject = true
}

/** A configuration with a single jar file containing the application domain model
 * (and server classes), used by the Framework Server module when running the server*/
val domain: Configuration by configurations.creating

tasks.register<Jar>("domainJar") {
    dependsOn("classes")
    group = "build"
    archiveBaseName = archiveBaseName.get() + "-domain"
    from(sourceSets.main.get().output)
    include("**/domain/**/*")
    include("**/migration/**/*")
    include("**/server/**/*")
    include("**/services/**/*")
    include("**/i18n/**/*")
    includeEmptyDirs = false
    manifest {
        attributes["Automatic-Module-Name"] = "is." + project.name.replace("-", ".") + ".domain"
    }
}

artifacts {
    add("domain", tasks.named("domainJar"))
}

tasks.register<WriteProperties>("writeVersion") {
    destinationFile = file(temporaryDir.absolutePath + "/version.properties")

    property("version", project.version)
}

tasks.processResources {
    from(tasks.named("writeVersion"))
}

apply(from = "../../plugins/jasperreports/extra-module-info-jasperreports.gradle")

jasperreports {
    classpath.from(sourceSets.main.get().compileClasspath)
}

sourceSets.main.get().resources.srcDir(tasks.compileAllReports)

tasks.withType<Test>().configureEach {
    systemProperty("codion.db.initScripts", "src/main/sql/create_schema.sql")
    systemProperty("codion.client.domainType", "Chinook")
}

tasks.register<JavaExec>("runClientLocal") {
    group = "application"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "is.codion.demos.chinook.ui.ChinookAppPanel"
    maxHeapSize = "128m"
    systemProperties = mapOf(
        "codion.client.connectionType" to "local",
        "codion.db.url" to "jdbc:h2:mem:h2db",
        "codion.db.initScripts" to "src/main/sql/create_schema.sql",
        "is.codion.swing.framework.ui.EntityTablePanel.includeQueryInspector" to "true",
        "is.codion.swing.framework.ui.EntityEditPanel.includeQueryInspector" to "true",
        "sun.awt.disablegrab" to "true"
    )
}

tasks.register<JavaExec>("runClientRMI") {
    group = "application"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "is.codion.demos.chinook.ui.ChinookAppPanel"
    maxHeapSize = "128m"
    systemProperties = mapOf(
        "codion.client.connectionType" to "remote",
        "codion.server.hostname" to properties["serverHostName"],
        "codion.client.trustStore" to "../../framework/server/src/main/config/truststore.jks",
        "sun.awt.disablegrab" to "true"
    )
}

tasks.register<JavaExec>("runClientHttp") {
    group = "application"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "is.codion.demos.chinook.ui.ChinookAppPanel"
    maxHeapSize = "128m"
    systemProperties = mapOf(
        "codion.client.connectionType" to "http",
        "codion.client.http.secure" to "false",
        "codion.client.http.hostname" to properties["serverHostName"],
        "sun.awt.disablegrab" to "true"
    )
}

tasks.register<JavaExec>("runLoadTestRMI") {
    group = "application"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "is.codion.demos.chinook.testing.ChinookLoadTest"
    maxHeapSize = "512m"
    systemProperties = mapOf(
        "codion.client.connectionType" to "remote",
        "codion.server.hostname" to properties["serverHostName"],
        "codion.client.trustStore" to "../../framework/server/src/main/config/truststore.jks"
    )
}

tasks.register<JavaExec>("runLoadTestHttp") {
    group = "application"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "is.codion.demos.chinook.testing.ChinookLoadTest"
    maxHeapSize = "512m"
    systemProperties = mapOf(
        "codion.client.connectionType" to "http",
        "codion.client.http.secure" to "false",
        "codion.client.http.hostname" to properties["serverHostName"]
    )
}