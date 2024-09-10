dependencies {
    api(project(":codion-framework-i18n"))
    api(project(":codion-swing-common-ui"))
    api(project(":codion-swing-framework-model"))

    implementation(libs.ikonli.swing)

    implementation(libs.json)

    implementation(libs.slf4j.api)

    testImplementation(project(":codion-framework-db-local"))

    testRuntimeOnly(project(":codion-dbms-h2"))
    testRuntimeOnly(libs.h2)
}