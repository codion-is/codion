dependencies {
    api(project(":codion-common-db"))

    // The driver's Function, by which upper() and lower() are registered on a connection, the driver itself the
    // application's, as with every dbms module
    compileOnly(libs.sqlite)

    testRuntimeOnly(libs.sqlite)
}