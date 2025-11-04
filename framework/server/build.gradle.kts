dependencies {
    api(project(":codion-common-rmi"))
    api(project(":codion-framework-db-core"))

    implementation(project(":codion-framework-db-local"))
    implementation(project(":codion-framework-db-rmi"))
    implementation(project(":codion-tools-jul-classpath"));

    implementation(libs.slf4j.api)

    testRuntimeOnly(project(":codion-plugin-hikari-pool"))
    testRuntimeOnly(project(":codion-dbms-h2"))
    testRuntimeOnly(libs.h2)
}