plugins {
    id("org.gradlex.extra-java-module-info")
}

dependencies {
    implementation(project(":codion-framework-db-rmi"))
    implementation(project(":codion-framework-server"))

    implementation(libs.slf4j.api)

    // AWS Lambda runtime - marked as compileOnly since the application will provide these
    compileOnly(libs.aws.lambda.core)
    compileOnly(libs.aws.lambda.events)

    testImplementation(project(":codion-framework-db-http"))
    testImplementation(libs.aws.lambda.core)
    testImplementation(libs.aws.lambda.events)
    testRuntimeOnly(project(":codion-dbms-h2"))
    testRuntimeOnly(libs.h2)
}

extraJavaModuleInfo {
    automaticModule("com.amazonaws:aws-lambda-java-core", "aws.lambda.java.core")
    automaticModule("com.amazonaws:aws-lambda-java-events", "aws.lambda.java.events")
}