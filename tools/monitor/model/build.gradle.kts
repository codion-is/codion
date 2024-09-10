dependencies {
    api(project(":codion-swing-common-model"))
    api(project(":codion-framework-db-rmi"))
    api(project(":codion-framework-server"))

    implementation(libs.slf4j.api)

    api(libs.jfreechart)

    runtimeOnly(project(":codion-plugin-logback-proxy"))

    testImplementation(project(":codion-framework-db-rmi"))
    testRuntimeOnly(project(":codion-plugin-hikari-pool"))
    testRuntimeOnly(project(":codion-dbms-h2"))
    testRuntimeOnly(libs.h2)
}