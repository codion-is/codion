#### JMinor Quickstart

## Rich client
JMinor is primarily a Swing based framework, the Swing client is very mature and stable while the JavaFX client is quite rudimentary and still in the 'proof-of-concept' stage.

|Client|Artifact|
|---|---
|Swing|org.jminor:jminor-swing-framework.ui:0.12.2|
|JavaFX|org.jminor:jminor-javafx-framework:0.12.2|

## Database connectivity
A JMinor client has three ways of connecting to a database, directly via a local JDBC connection or remotely via RMI or HTTP using the JMinor remote server.

|DB Connection|Artifact|
|---|---
|Local|org.jminor:jminor-framework-db-local:0.12.2|
|RMI|org.jminor:jminor-framework-db-remote:0.12.2|
|HTTP|org.jminor:jminor-framework-db-http:0.12.2|

## DBMS
When connecting to the database with a local JDBC connection the DBMS module for the underlying database must be on the classpath. Note that these artifacts do not depend on the JDBC drivers, so those must be added separately.

The most used and tested DBMS modules are:

1. Oracle
2. H2 Database
3. Postgresql

|DBMS|Artifact|
|---|---|
|Derby|org.jminor:jminor-dbms-derby:0.12.2|
|H2 Database|org.jminor:jminor-dbms-h2database:0.12.2|
|HSQL|org.jminor:jminor-dbms-hsql:0.12.2|
|MySQL|org.jminor:jminor-dbms-mysql:0.12.2|
|Oracle|org.jminor:jminor-dbms-oracle:0.12.2|
|Postgresql|org.jminor:jminor-dbms-postgresql:0.12.2|
|SQLite|org.jminor:jminor-dbms-sqlite:0.12.2|
|SQL Server|org.jminor:jminor-dbms-sqlserver:0.12.2|

## Logging
JMinor uses SLF4J throughout so all you need to do is add a SLF4J bridge for your logging framework of choice to your classpath. If you use Logback or Log4J you can use one of the logging-proxy plugins below which will pull in the required dependencies and also allow you to set the logging level in the client.

|Logging|Artifact|
|---|---
|Logback|org.jminor:jminor-plugin-logback-proxy:0.12.2|
|Log4j|org.jminor:jminor-plugin-log4j-proxy:0.12.2|

### Gradle
```groovy
dependencies {
    //Swing client UI module
    compile 'org.jminor:jminor-swing-framework-ui:0.12.2'
    
    //JSON for persisting client configuration
    runtime 'org.jminor:jminor-framework-plugins-json:0.12.2'    
    //Local JDBC connection module
    runtime 'org.jminor:jminor-framework-db-local:0.12.2'
    //H2 DBMS module
    runtime 'org.jminor:jminor-dbms-h2database:0.12.2'
    //H2 JDBC driver
    runtime 'com.h2database:h2:1.4.199'

    //Domain model unit testing module
    testCompile 'org.jminor:jminor-framework-domain-test:0.12.2'
    //JUnit
    testCompile 'org.junit.jupiter:junit-jupiter-api:5.5.1'
    testRuntime 'org.junit.jupiter:junit-jupiter-engine:5.5.1'
}
```