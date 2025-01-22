dependencies {
    implementation(project(":codion-swing-common-ui"))
    implementation(project(":codion-tools-monitor-model"))
    implementation(project(":codion-plugin-flatlaf"))

    implementation(libs.slf4j.api)
    implementation(libs.jfreechart)

    testRuntimeOnly(project(":codion-framework-db-rmi"))
    testRuntimeOnly(project(":codion-plugin-hikari-pool"))
    testRuntimeOnly(project(":codion-dbms-h2"))
    testRuntimeOnly(libs.h2)
}