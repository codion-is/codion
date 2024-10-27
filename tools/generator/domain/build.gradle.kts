dependencies {
    api(project(":codion-framework-domain"))

    implementation(libs.javapoet)

    testImplementation(project(":codion-framework-domain-db"))
    testRuntimeOnly(project(":codion-dbms-h2"))
    testRuntimeOnly(libs.h2)
}