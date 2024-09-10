dependencies {
    api(project(":codion-common-db"))
    api(project(":codion-swing-common-model"))

    api(libs.jfreechart)

    testRuntimeOnly(project(":codion-plugin-hikari-pool"))
    testRuntimeOnly(project(":codion-dbms-h2"))
    testRuntimeOnly(libs.h2)
}