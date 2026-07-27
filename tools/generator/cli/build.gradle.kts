plugins {
    application
}

dependencies {
    implementation(project(":codion-tools-generator-domain"))
    implementation(project(":codion-framework-domain-db"))

    // The dbms module and the JDBC driver are supplied by whoever runs the CLI, add the
    // ones required to this build file before running the installDist task.

    // runtimeOnly(project(":codion-dbms-h2"))
    // runtimeOnly(libs.h2)

    // runtimeOnly(project(":codion-dbms-postgresql"))
    // runtimeOnly("org.postgresql:postgresql:42.7.3")

    testRuntimeOnly(project(":codion-dbms-h2"))
    testRuntimeOnly(libs.h2)
}

application {
    mainModule = "is.codion.tools.generator.cli"
    mainClass = "is.codion.tools.generator.cli.DomainGeneratorCli"
}