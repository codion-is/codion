dependencies {
    api(project(":codion-common-db"))

    implementation(libs.hikari)

    testImplementation(project(":codion-dbms-h2"))
    testRuntimeOnly(libs.h2)
}