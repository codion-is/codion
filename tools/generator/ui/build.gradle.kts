plugins {
    application
}

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

    testRuntimeOnly(project(":codion-framework-db-local"))
    testRuntimeOnly(project(":codion-framework-db-rmi"))
    testRuntimeOnly(project(":codion-framework-server"))
}

application {
    mainClass = "is.codion.tools.generator.ui.DomainGeneratorPanel"
    applicationDefaultJvmArgs = listOf(
        "-Xmx256m",
        "-Dcodion.domain.generator.defaultDomainPackage=is.codion.demo.domain"
    )
    applicationDefaultJvmArgs += listOf(
        "-Dcodion.db.url=jdbc:h2:mem:h2db",
        "-Dcodion.db.initScripts=" +
                "../../../demos/chinook/src/main/sql/create_schema.sql," +
                "../../../demos/employees/src/main/sql/create_schema.sql," +
                "../../../demos/petclinic/src/main/sql/create_schema.sql," +
                "../../../demos/petstore/src/main/sql/create_schema.sql," +
                "../../../demos/world/src/main/sql/create_schema.sql",
        "-Dcodion.domain.generator.defaultUser=sa",
    )
//    applicationDefaultJvmArgs += listOf(
//        "-Dcodion.db.url=jdbc:postgresql://localhost:5432/postgres"
//    )
//    applicationDefaultJvmArgs += listOf(
//        "-Dcodion.db.url=jdbc:oracle:thin:@localhost:1521:sid"
//    )
//    applicationDefaultJvmArgs += listOf(
//        "-Dcodion.db.url=jdbc:mariadb://localhost:3306/mariadb"
//    )
}