dependencies {
    api(project(":codion-common-db"))
    api(project(":codion-swing-common-model"))

    implementation(project(":codion-framework-domain-db"))
    implementation(project(":codion-tools-generator-domain"))

    testRuntimeOnly(project(":codion-dbms-h2"))
    testRuntimeOnly(libs.h2)
}