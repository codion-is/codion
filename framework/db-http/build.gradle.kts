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
    // CI runners occasionally exceed the 2s default on a localhost connect, and since building a
    // connection now connects eagerly, every test method opens one - see codion.client.http.connectTimeout
    systemProperty("codion.client.http.connectTimeout", "10000")
    systemProperty("codion.client.http.socketTimeout", "10000")
}
