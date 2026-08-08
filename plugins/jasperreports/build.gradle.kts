plugins {
    id("org.gradlex.extra-java-module-info")
    id("io.github.f-cramer.jasperreports")
}

dependencies {
    api(project(":codion-common-db"))

    api(libs.jasperreports) {
        exclude(group = "xml-apis")
    }
    jasperreportsClasspath(libs.jasperreports.jdt) {
        exclude(group = "xml-apis")
    }
    testImplementation(project(":codion-framework-db-local"))
    testImplementation(project(":codion-dbms-h2"))
    testImplementation(libs.javalin) {
        exclude(group = "org.jetbrains", module = "annotations")
    }
    // JasperReportsWireTest: a real server filling and exporting a report, a remote client receiving the bytes
    testImplementation(project(":codion-common-rmi"))
    testImplementation(project(":codion-framework-db-http"))
    testImplementation(project(":codion-framework-server"))
    testImplementation(project(":codion-framework-servlet"))
    testImplementation(project(":codion-framework-json-domain"))
    testRuntimeOnly(libs.h2)
    // JRExport.PDF resolves its exporter through the JasperReports extension registry,
    // this artifact is not a dependency of the plugin, a consumer wanting PDF adds it
    testRuntimeOnly(libs.jasperreports.pdf) {
        exclude(group = "xml-apis")
    }
}

apply(from = "extra-module-info-jasperreports.gradle")

jasperreports {
    srcDir = file("src/test/reports")
    classpath.from(sourceSets.main.get().compileClasspath)
}

sourceSets.test.get().resources.srcDir(tasks.compileAllReports)

tasks.test {
    // The default is no socket timeout, matching RMI - right for an application, wrong for a test run, where a
    // server that accepts the connection and never answers should fail the test rather than hang the build.
    systemProperty("codion.client.http.socketTimeout", "30000")
}
