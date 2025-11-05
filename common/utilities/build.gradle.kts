dependencies {
    api(project(":codion-common-reactive"))
}

tasks.register<WriteProperties>("writeVersion") {
    group = "build"
    description = "Writes the current framework version to a file available as a resource"

    destinationFile = file(temporaryDir.absolutePath + "/version.properties")

    property("version", project.version)
}

tasks.withType<ProcessResources>().configureEach {
    from(tasks.named("writeVersion"))

    filesMatching("version.properties") {
        path = "/is/codion/common/utilities/version/$path"
    }
}