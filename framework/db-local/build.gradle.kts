dependencies {
    api(project(":codion-framework-db"))

    implementation(libs.slf4j.api)

    testImplementation(project(":codion-dbms-h2"))
    testRuntimeOnly(libs.h2)
}