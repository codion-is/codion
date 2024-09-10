dependencies {
    api(project(":codion-framework-model"))
    api(project(":codion-swing-common-model"))

    implementation(libs.slf4j.api)

    testImplementation(project(":codion-framework-db-local"))
    testImplementation(project(":codion-framework-model-test"))

    testRuntimeOnly(project(":codion-dbms-h2"))
    testRuntimeOnly(libs.h2)
}