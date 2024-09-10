tasks.register<WriteProperties>("writeVersion") {
    destinationFile = file(temporaryDir.absolutePath + "/version.properties")

    property("version", project.version)
}

tasks.withType<ProcessResources> {
    from(tasks.named("writeVersion"))

    filesMatching("version.properties") {
        path = "/is/codion/common/version/$path"
    }
}