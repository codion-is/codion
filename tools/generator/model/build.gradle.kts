dependencies {
    api(project(":codion-framework-domain-db"))
    api(project(":codion-framework-domain-test"))

    implementation(project(":codion-swing-common-model"))

    implementation(project(":codion-tools-generator-domain"))
    implementation(libs.json)

    testRuntimeOnly(project(":codion-dbms-h2"))
    testRuntimeOnly(libs.h2)
}