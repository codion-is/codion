dependencies {
    api(project(":codion-tools-generator-domain"))
    api(project(":codion-swing-common-model"))

    testRuntimeOnly(project(":codion-dbms-h2"))
    testRuntimeOnly(libs.h2)
}