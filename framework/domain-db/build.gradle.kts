dependencies {
    api(project(":codion-framework-domain"))

    implementation(libs.slf4j.api)

    testRuntimeOnly(project(":codion-dbms-h2"))
    testRuntimeOnly(libs.h2)
}