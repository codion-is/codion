dependencies {
    implementation(project(":codion-tools-loadtest-ui"))
    implementation(project(":codion-swing-framework-ui"))

    implementation(project(":codion-plugin-flatlaf"))
    implementation(project(":codion-plugin-flatlaf-intellij-themes"))

    runtimeOnly(project(":codion-plugin-logback-proxy"))

    runtimeOnly(project(":codion-framework-db-local"))
    runtimeOnly(project(":codion-framework-db-rmi"))
    runtimeOnly(project(":codion-framework-db-http"))

    testImplementation(project(":codion-framework-domain-test"))
    testImplementation(project(":codion-swing-framework-ui-test"))

    implementation(libs.jfreechart)

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

tasks.withType<Test>().configureEach {
    systemProperty(
        "codion.db.initScripts",
        "../chinook/src/main/sql/create_schema.sql," +
                "../employees/src/main/sql/create_schema.sql," +
                "../petclinic/src/main/sql/create_schema.sql," +
                "../petstore/src/main/sql/create_schema.sql," +
                "../world/src/main/sql/create_schema.sql"
    )
    systemProperty("codion.client.domainType", "SchemaBrowser")
}

tasks.register<JavaExec>("runClientLocal") {
    group = "application"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "is.codion.demos.schemabrowser.client.ui.SchemaBrowserAppPanel"
    systemProperties = mapOf(
        "codion.client.connectionType" to "local",
        "codion.db.url" to "jdbc:h2:mem:h2db",
        "codion.db.initScripts" to "../chinook/src/main/sql/create_schema.sql," +
                "../employees/src/main/sql/create_schema.sql," +
                "../petclinic/src/main/sql/create_schema.sql," +
                "../petstore/src/main/sql/create_schema.sql," +
                "../world/src/main/sql/create_schema.sql"
    )
}