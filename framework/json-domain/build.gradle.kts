dependencies {
    implementation(project(":codion-framework-domain"))

    api(libs.jackson.databind)
    api(libs.jackson.datatype.jsr310)
}