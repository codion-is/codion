dependencies {
    implementation(project(":codion-framework-domain"))

    implementation(libs.slf4j.api)

    api(libs.jackson.databind)
    api(libs.jackson.datatype.jsr310)
}