dependencies {
    implementation(project(":codion-framework-db-core"))

    api(project(":codion-framework-json-domain"))

    api(libs.jackson.databind)
    api(libs.jackson.datatype.jsr310)
}