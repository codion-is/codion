dependencies {
    implementation(project(":codion-swing-framework-ui"))

    runtimeOnly(project(":codion-plugin-logback-proxy"))
    runtimeOnly(project(":codion-framework-db-local"))

    runtimeOnly(project(":codion-dbms-h2"))
    runtimeOnly(libs.h2)
}

sonarqube {
    isSkipProject = true
}

tasks.register<WriteProperties>("writeVersion") {
    destinationFile = file(temporaryDir.absolutePath + "/version.properties")

    property("version", project.version)
}

tasks.processResources {
    from(tasks.named("writeVersion"))
}

tasks.register<JavaExec>("runClientLocal") {
    group = "application"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "is.codion.demos.schemabrowser.client.ui.SchemaBrowserAppPanel"

    // H2Database does not allow path traversal in init scripts
    val scriptPaths = listOf(
        project(":demo-chinook").file("src/main/sql/create_schema.sql"),
        project(":demo-employees").file("src/main/sql/create_schema.sql"),
        project(":demo-petclinic").file("src/main/sql/create_schema.sql"),
        project(":demo-petstore").file("src/main/sql/create_schema.sql"),
        project(":demo-world").file("src/main/sql/create_schema.sql")
    ).joinToString(",") { it.absolutePath }

    systemProperties = mapOf(
        "codion.client.connectionType" to "local",
        "codion.db.url" to "jdbc:h2:mem:h2db",
        "codion.db.initScripts" to "$scriptPaths",
    )
}