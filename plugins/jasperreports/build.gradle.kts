plugins {
    id("org.gradlex.extra-java-module-info")
    id("io.github.f-cramer.jasperreports")
}

dependencies {
    api(project(":codion-common-db"))

    api(libs.jasperreports) {
        exclude(group = "xml-apis")
    }
    compileOnly(libs.jasperreports.jdt) {
        exclude(group = "xml-apis")
    }

    testImplementation(project(":codion-framework-db-local"))
    testImplementation(project(":codion-dbms-h2"))
    testImplementation(libs.javalin) {
        exclude(group = "org.jetbrains", module = "annotations")
    }
    testRuntimeOnly(libs.h2)
}

apply(from = "extra-module-info-jasperreports.gradle")

jasperreports {
    srcDir = file("src/test/reports")
    classpath.from(sourceSets["main"].compileClasspath)
}

sourceSets["test"].resources.srcDir(tasks.named("compileAllReports"))