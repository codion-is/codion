JMinor Application Framework

http://jminor.org


1. INTRODUCTION

JMinor is a minimalistic full-stack Java rich client CRUD application framework based solely on J2SE components, it includes:

* Simple domain modelling in plain Java code, no XML files required.

* Integrated JUnit testing of the domain model.

* A minimal but complete JDBC abstraction layer.

* A straight forward and simple way of wiring together a rich Swing client on top of the domain model,
  all done in plain Java code, no XML configuration involved.

* Clients are run with either a local JDBC connection or served by a featherweight RMI server.

* Integrated JasperReports support.

* Logging provided by the Logback logging framework.


2. RELEASE INFO

Release contents (~10 MB):
* "dist" contains the JMinor binary jar files
* "docs" contains basic documentation as well as API javadocs
* "projects" contains project files for the IntelliJ and Netbeans IDEs
* "resources" contains miscellaneous files required for running the JMinor server, the server monitor
              and the demo applications. resources/project_template contains an ant build file template
              for projects using the JMinor framework.
* "src" contains the Java source files for the framework
* "srcdemos" contains the Java source files for the framework demos: EmpDept, Petstore, Chinook and SchemaBrowser
* "srctest" contains the JUnit test sources for the framework
* "build.xml" ant build file
* "build.properties" ant build properties
* "ivy.xml" ivy dependency management configuration (ant->resolve_libraries)
* "changelog.txt" the framework changelog


3. DISTRIBUTION JAR FILES

The "dist" directory contains the following distinct jar files for use in applications.

* "jminor.jar" (~1.2 MB)
- Convenience jar file containing the full framework codebase

* "jminor-common.jar" (~515 KB)
- Common codebase

* "jminor-db.jar" (~170 KB)
- Database layer code
- Dependencies: jminor-common.jar

* "jminor-db-provider.jar" (~20 KB)
- Database access layer code, remote and local
- Dependencies: jminor-common.jar, jminor-db.jar

* "jminor-client.jar" (~340 KB)
- Full client codebase
- Dependencies: jminor-common.jar, jminor-db.jar, jminor-db-remote.jar (if remote connections are required)

* "jminor-plugins.jar" (~25 KB)
- Framework plugin codebase, JasperReports, Tomcat connection pool, EntityJSONParser and EntityRESTService
- Dependencies: jminor-common.jar

* "jminor-server.jar" (~35 KB)
- RMI server codebase
- Dependencies: jminor-common.jar, jminor-db.jar

* "jminor_server_monitor.jar" (~85 KB)
- RMI server monitor codebase
- Dependencies: jminor-common.jar

* "jminor-demos.jar" (~130 KB)
- Three demo applications
- Dependencies: jminor-common.jar, jminor-db.jar, jminor-db-provider.jar, jminor-client.jar

* "jminor-android.jar" (~300 KB)
- Android tailored library

* "jminor-api-doc.jar" (~2.5 MB)
- API documentation


4. GETTING STARTED

JMinor comes with four demo applications, a small one based on the SCOTT schema found in most if not all Oracle
setups called EmpDept, a larger one called Petstore based on a schema of the same name, an application based on
a music web-store schema called Chinook and a SchemaBrowser, which is limited to Oracle, MySQL and H2 databases.
An H2 database containing the required schemas can be generated via the ant target 'recreate_h2_db'.

In order to run the demos you must first run the deploy_all target in the ant build file (info on how to run ant
builds can be found at ant.apache.org), which deploys the demo applications into the folder dist/deployment along
with the required database.

dist/deployment
  '-jminor_demos          - application jar files, the H2 database, files to run the demo applications
  '-jminor_server         - server jar files, the H2 database, files to run the server
  '-jminor_server_monitor - server monitor jar files, files to run the server monitor

The demo applications can be run with an embedded H2 database or using the JMinor RMI Server.

To run the demo applications with the RMI server you must first start the server by executing one of the run files
in the 'jminor_server' directory, with the server running you can start the remote demo client by executing one of
the ...client_remote.bat/sh run files in the 'jminor_demos' directory.


5. BUILDING THE PROJECT

The Ant build file included in the project contains standard targets for building the project, running unit tests
and packaging the compiled classes as well a targets for running the server and demo applications and creating the
required databases.

Before you can use the 'sign_*_jars targets' the you must create a file called 'user.properties' in the project
directory, and in it define the following build properties:

jar.sign.alias            - the keystore alias
jar.sign.storepass        - the keystore password
jar.sign.keystore         - the keystore path

Before you can run the unit tests you must define the following properties in 'user.properties':

jacocoant.jar             - the path to the jacocoant.jar file for the JaCoCo coverage library
ivy.jar                   - the path to the ivy.jar file for the ivy dependency management library
getdown.jar               - the path to the getdown.jar file for the getdown distribution management library