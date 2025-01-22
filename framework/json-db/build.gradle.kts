dependencies {
    api(project(":codion-framework-json-domain"))

    implementation(project(":codion-framework-db-core"))

    api(libs.jackson.databind)
    api(libs.jackson.datatype.jsr310)
}