dependencies {
    api(project(":codion-framework-json-domain"))

    implementation(project(":codion-framework-db"))

    api(libs.jackson.databind)
    api(libs.jackson.datatype.jsr310)
}