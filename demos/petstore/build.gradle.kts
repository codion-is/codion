dependencies {
    implementation(project(":codion-tools-loadtest-ui"))
    implementation(project(":codion-swing-framework-ui"))

    runtimeOnly(project(":codion-plugin-logback-proxy"))

    runtimeOnly(project(":codion-framework-db-local"))
    runtimeOnly(project(":codion-framework-db-rmi"))
    runtimeOnly(project(":codion-framework-db-http"))

    testImplementation(project(":codion-framework-domain-test"))

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
    include("**/services/**/*")
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

tasks.withType<Test>().configureEach {
    systemProperty("codion.db.initScripts", "src/main/sql/create_schema.sql")
    systemProperty("codion.client.domainType", "Petstore")
}

tasks.register<JavaExec>("runClientLocal") {
    group = "application"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "is.codion.demos.petstore.ui.PetstoreAppPanel"
    maxHeapSize = "128m"
    systemProperties = mapOf(
        "codion.client.connectionType" to "local",
        "codion.db.url" to "jdbc:h2:mem:h2db",
        "codion.db.initScripts" to "src/main/sql/create_schema.sql",
        "sun.awt.disablegrab" to "true"
    )
}

tasks.register<JavaExec>("runClientRMI") {
    group = "application"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "is.codion.demos.petstore.ui.PetstoreAppPanel"
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
    mainClass = "is.codion.demos.petstore.ui.PetstoreAppPanel"
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
    mainClass = "is.codion.demos.petstore.testing.PetstoreLoadTest"
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
    mainClass = "is.codion.demos.petstore.testing.PetstoreLoadTest"
    maxHeapSize = "512m"
    systemProperties = mapOf(
        "codion.client.connectionType" to "http",
        "codion.client.http.secure" to "false",
        "codion.client.http.hostname" to properties["serverHostName"]
    )
}