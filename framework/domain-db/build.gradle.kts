dependencies {
    api(project(":codion-framework-domain"))

    implementation(libs.slf4j.api)

    testImplementation(project(":codion-framework-domain-test"))
    testRuntimeOnly(project(":codion-dbms-h2"))
    testRuntimeOnly(libs.h2)
}