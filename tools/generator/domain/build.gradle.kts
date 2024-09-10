dependencies {
    api(project(":codion-framework-domain-db"))

    implementation(libs.javapoet)

    testRuntimeOnly(project(":codion-dbms-h2"))
    testRuntimeOnly(libs.h2)
}