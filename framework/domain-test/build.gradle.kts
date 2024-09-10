dependencies {
    api(project(":codion-framework-db-core"))
    implementation(project(":codion-framework-db-local"))

    api(libs.junit.api)

    implementation(libs.slf4j.api)
}