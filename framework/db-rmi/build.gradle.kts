dependencies {
    api(project(":codion-common-rmi"))
    api(project(":codion-framework-db-core"))

    implementation(libs.slf4j.api)

    testImplementation(project(":codion-framework-server"))
    testRuntimeOnly(project(":codion-dbms-h2"))
    testRuntimeOnly(libs.h2)
}