plugins {
    id("org.gradlex.extra-java-module-info")
}

dependencies {
    api(project(":codion-common-db"))

    implementation(libs.tomcat.jdbc)
    implementation(libs.slf4j.api)

    testImplementation(project(":codion-dbms-h2"))
    testRuntimeOnly(libs.h2)
}

extraJavaModuleInfo {
    automaticModule("org.apache.tomcat:tomcat-jdbc", "tomcat.jdbc")
}