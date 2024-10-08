dependencies {
    api(project(":codion-common-model"))
    api(project(":codion-framework-db-core"))

    testImplementation(project(":codion-framework-db-local"))
    testImplementation(project(":codion-framework-model-test"))

    implementation(libs.slf4j.api)

    testRuntimeOnly(project(":codion-dbms-h2"))
    testRuntimeOnly(libs.h2)
}