plugins {
    application
}

dependencies {
    implementation(project(":codion-framework-domain"))
    implementation(project(":codion-tools-generator-ui"))

    runtimeOnly(project(":codion-dbms-h2"))
    runtimeOnly(libs.h2)

//    runtimeOnly(project(":codion-dbms-postgresql"))
//    runtimeOnly("org.postgresql:postgresql:42.7.3")

//    runtimeOnly(project(":codion-dbms-oracle"))
//    runtimeOnly("com.oracle.database.jdbc:ojdbc:23.3.0.23.09") { isTransitive = false }

//    runtimeOnly(project(":codion-dbms-mariadb"))
//    runtimeOnly("org.mariadb.jdbc:mariadb-java-client:3.4.1")
}

sonarqube {
    isSkipProject = true
}

application {
    mainModule = "is.codion.tools.generator.ui"
    mainClass = "is.codion.tools.generator.ui.DomainGeneratorPanel"

    // H2Database does not allow path traversal in init scripts
    val scriptPaths = listOf(
        project(":demo-chinook").file("src/main/sql/create_schema.sql"),
        project(":demo-employees").file("src/main/sql/create_schema.sql"),
        project(":demo-petclinic").file("src/main/sql/create_schema.sql"),
        project(":demo-petstore").file("src/main/sql/create_schema.sql"),
        project(":demo-world").file("src/main/sql/create_schema.sql")
    ).joinToString(",") { it.absolutePath }

    applicationDefaultJvmArgs = listOf(
        "-Xmx256m",
        "-Dcodion.domain.generator.domainPackage=is.codion.demo.domain"
    )
    applicationDefaultJvmArgs += listOf(
        "-Dcodion.db.url=jdbc:h2:mem:h2db",
        "-Dcodion.db.initScripts=$scriptPaths",
        "-Dcodion.domain.generator.user=sa",
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