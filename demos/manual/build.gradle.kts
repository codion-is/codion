plugins {
    id("org.gradlex.extra-java-module-info")
}

dependencies {
    implementation(project(":codion-tools-loadtest-ui"))
    implementation(project(":codion-swing-framework-ui"))
    implementation(project(":codion-framework-domain-test"))
    implementation(project(":codion-framework-db-local"))
    implementation(project(":codion-framework-db-rmi"))
    implementation(project(":codion-framework-db-http"))
    implementation(project(":codion-plugin-jasperreports")) {
        exclude(group = "org.apache.xmlgraphics")
    }

    implementation(project(":codion-framework-server"))
    implementation(project(":codion-framework-servlet"))
    implementation(project(":codion-dbms-h2"))

    implementation(project(":codion-plugin-flatlaf-themes"))
    implementation(project(":codion-plugin-flatlaf-intellij-themes"))

    implementation(libs.jasperreports) {
        isTransitive = false
    }

    runtimeOnly(project(":codion-plugin-logback-proxy"))

    runtimeOnly(libs.h2)
}

sonarqube {
    isSkipProject = true
}

apply(from = "../../plugins/jasperreports/extra-module-info-jasperreports.gradle")

tasks.test {
    systemProperty("codion.db.initScripts", "src/main/sql/create_schema.sql")
    // ReadmeTest verifies the code blocks in the readme against the files they were copied from
    inputs.file(rootProject.file("readme.adoc")).withPropertyName("readme")
    filter {
        // The readme application has a schema of its own, incompatible with the store application
        excludeTestsMatching("is.codion.manual.app.readme.domain.StoreTest")
    }
}

tasks.register<JavaExec>("runStoreDemo") {
    group = "application"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "is.codion.manual.app.readme.ui.StoreDemo"
}

tasks.register<JavaExec>("runNotesDemo") {
    group = "application"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "is.codion.manual.app.notes.NotesDemo"
}

tasks.register<JavaExec>("runApplicationPanel") {
    group = "application"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "is.codion.manual.app.components.ApplicationPanel"
}

tasks.register<JavaExec>("runKeyBindingPanel") {
    group = "application"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "is.codion.manual.app.keybinding.KeyBindingPanel"
}