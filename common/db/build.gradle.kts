dependencies {
    api(project(":codion-common-core"))

    testImplementation(project(":codion-dbms-h2"))

    testRuntimeOnly(project(":codion-plugin-hikari-pool"))
    testRuntimeOnly(libs.h2)
}