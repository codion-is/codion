dependencies {
    api(project(":codion-framework-db"))

    implementation(project(":codion-framework-db-local"))

    api(libs.junit.api)

    implementation(libs.slf4j.api)
}