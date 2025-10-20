dependencies {
    api(project(":codion-tools-generator-model"))

    implementation(project(":codion-swing-common-ui"))
    implementation(project(":codion-plugin-flatlaf"))

    testRuntimeOnly(project(":codion-framework-db-local"))
    testRuntimeOnly(project(":codion-framework-db-rmi"))
    testRuntimeOnly(project(":codion-framework-server"))
    testRuntimeOnly(project(":codion-dbms-h2"))
    testRuntimeOnly(libs.h2)
}