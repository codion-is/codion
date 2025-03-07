dependencies {
    implementation(project(":codion-framework-db-rmi"))
    implementation(project(":codion-framework-json-domain"))
    implementation(project(":codion-framework-json-db"))

    implementation(libs.javalin) {
        exclude(group = "org.jetbrains", module = "annotations")
    }
    implementation(libs.javalin.ssl) {
        exclude(group = "org.jetbrains", module = "annotations")
    }

    implementation(libs.slf4j.api)

    testImplementation(project(":codion-framework-server"))

    testRuntimeOnly(project(":codion-framework-db-local"))
    testRuntimeOnly(project(":codion-plugin-hikari-pool"))
    testRuntimeOnly(project(":codion-dbms-h2"))
    testRuntimeOnly(libs.h2)
}