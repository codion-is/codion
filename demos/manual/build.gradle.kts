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

    implementation(project(":codion-plugin-intellij-themes"))

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

tasks.withType<Test>().configureEach {
    systemProperty("codion.db.initScripts", "src/main/sql/create_schema.sql")
}

tasks.register<JavaExec>("runStoreDemo") {
    group = "application"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "is.codion.manual.store.minimal.ui.StoreDemo"
}

tasks.register<JavaExec>("runNotesDemo") {
    group = "application"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "is.codion.manual.notes.NotesDemo"
}

tasks.register<JavaExec>("runApplicationPanel") {
    group = "application"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "is.codion.manual.common.demo.ApplicationPanel"
}

tasks.register<JavaExec>("runKeyBindingPanel") {
    group = "application"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "is.codion.manual.keybinding.KeyBindingPanel"
}