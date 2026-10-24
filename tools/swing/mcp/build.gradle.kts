plugins {
    application
}

dependencies {
    implementation(project(":codion-swing-common-ui"))
    implementation(project(":codion-tools-swing-robot"))

    implementation(libs.jackson.databind)
    implementation(libs.slf4j.api)
}

application {
    mainClass = "is.codion.tools.swing.mcp.SwingMcpBridge"
}