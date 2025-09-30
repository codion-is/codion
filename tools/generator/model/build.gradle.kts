dependencies {
    api(project(":codion-common-db"))
    api(project(":codion-swing-common-model"))
    api(project(":codion-framework-domain-db"))

    implementation(project(":codion-tools-generator-domain"))
    implementation(libs.json)

    testRuntimeOnly(project(":codion-dbms-h2"))
    testRuntimeOnly(libs.h2)
}