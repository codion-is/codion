dependencies {
    implementation(project(":codion-swing-common-ui"))
    implementation(project(":codion-tools-generator-model"))

    implementation(libs.flatlaf.intellij.themes)

    runtimeOnly(project(":codion-dbms-h2"))
    runtimeOnly(libs.h2)

//    runtimeOnly(project(":codion-dbms-postgresql"))
//    runtimeOnly("org.postgresql:postgresql:42.7.3")

//    runtimeOnly(project(":codion-dbms-oracle"))
//    runtimeOnly("com.oracle.database.jdbc:ojdbc:23.3.0.23.09") { isTransitive = false }

//    runtimeOnly(project(":codion-dbms-mariadb"))
//    runtimeOnly("org.mariadb.jdbc:mariadb-java-client:3.4.1")

//    runtimeOnly(project(":codion-dbms-db2database"))
//    runtimeOnly("com.ibm.db2:jcc:11.5.9.0")

    testRuntimeOnly(project(":codion-framework-db-local"))
    testRuntimeOnly(project(":codion-framework-db-rmi"))
    testRuntimeOnly(project(":codion-framework-server"))
}

tasks.register<JavaExec>("runDomainGeneratorH2") {
    group = "application"
    mainClass.set("is.codion.tools.generator.ui.DomainGeneratorPanel")
    classpath = sourceSets["main"].runtimeClasspath
    maxHeapSize = "256m"
    systemProperties = mapOf(
        "codion.db.url" to "jdbc:h2:mem:h2db",
        "codion.db.initScripts" to
                "../../../demos/chinook/src/main/sql/create_schema.sql," +
                "../../../demos/employees/src/main/sql/create_schema.sql," +
                "../../../demos/petclinic/src/main/sql/create_schema.sql," +
                "../../../demos/petstore/src/main/sql/create_schema.sql," +
                "../../../demos/world/src/main/sql/create_schema.sql",
        "codion.domain.generator.defaultUser" to "sa",
        "codion.domain.generator.defaultDomainPackage" to "is.codion.demo.domain"
    )
}

//tasks.register<JavaExec>("runDomainGeneratorOracle") {
//    group = "application"
//    mainClass.set("is.codion.tools.generator.ui.DomainGeneratorPanel")
//    classpath = sourceSets["main"].runtimeClasspath
//    maxHeapSize = "256m"
//    systemProperties = mapOf(
//        "codion.db.url" to "jdbc:oracle:thin:@localhost:1521:sid",
//        "codion.domain.generator.defaultDomainPackage" to "is.codion.demo.domain"
//    )
//}

//tasks.register<JavaExec>("runDomainGeneratorPostgres") {
//    group = "application"
//    mainClass.set("is.codion.tools.generator.ui.DomainGeneratorPanel")
//    classpath = sourceSets["main"].runtimeClasspath
//    maxHeapSize = "256m"
//    systemProperties = mapOf(
//        "codion.db.url" to "jdbc:postgresql://localhost:5432/postgres",
//        "codion.domain.generator.defaultDomainPackage" to "is.codion.demo.domain"
//    )
//}

//tasks.register<JavaExec>("runDomainGeneratorMariaDB") {
//    group = "application"
//    mainClass.set("is.codion.tools.generator.ui.DomainGeneratorPanel")
//    classpath = sourceSets["main"].runtimeClasspath
//    maxHeapSize = "256m"
//    systemProperties = mapOf(
//        "codion.db.url" to "jdbc:mariadb://localhost:3306/mariadb",
//        "codion.domain.generator.defaultDomainPackage" to "is.codion.demo.domain"
//    )
//}