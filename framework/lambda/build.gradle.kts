plugins {
    id("org.gradlex.extra-java-module-info")
}

dependencies {
    api(project(":codion-framework-domain"))
    api(project(":codion-framework-db-local"))
    
    // AWS Lambda runtime - marked as compileOnly since the application will provide these
    compileOnly("com.amazonaws:aws-lambda-java-core:1.2.3")
    compileOnly("com.amazonaws:aws-lambda-java-events:3.11.4")
    
    testImplementation(project(":codion-framework-db-http"))
    testImplementation("com.amazonaws:aws-lambda-java-core:1.2.3")
    testImplementation("com.amazonaws:aws-lambda-java-events:3.11.4")
    testRuntimeOnly(project(":codion-dbms-h2"))
    testRuntimeOnly(libs.h2)
}

extraJavaModuleInfo {
    automaticModule("com.amazonaws:aws-lambda-java-core", "aws.lambda.java.core")
    automaticModule("com.amazonaws:aws-lambda-java-events", "aws.lambda.java.events")
}