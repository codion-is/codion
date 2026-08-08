dependencies {
    api(project(":codion-framework-db-core"))

    implementation(project(":codion-framework-json-domain"))
    implementation(project(":codion-framework-json-db"))

    implementation(libs.slf4j.api)

    testImplementation(project(":codion-common-rmi"))
    testImplementation(project(":codion-framework-server"))
    testImplementation(project(":codion-framework-servlet"))

    testRuntimeOnly(project(":codion-plugin-hikari-pool"))
    testRuntimeOnly(project(":codion-dbms-h2"))
    testRuntimeOnly(libs.h2)
}

tasks.test {
    // The default is no socket timeout, matching RMI - right for an application, wrong for a test run, where a
    // server that accepts the connection and never answers should fail the test rather than hang the build.
    systemProperty("codion.client.http.socketTimeout", "30000")
}
