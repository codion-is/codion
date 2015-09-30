JMinor Application Framework

http://jminor.org


1. INTRODUCTION

JMinor is a minimalistic full-stack Java rich client CRUD application framework based solely on J2SE components, it includes:

* Simple domain modelling in plain Java code.

* Integrated JUnit testing of the domain model.

* A minimal but complete JDBC abstraction layer.

* A straight forward and simple way of wiring together a rich Swing client on top of the domain model,
  all done in plain Java code.

* Clients are run with either a local JDBC connection or served by a featherweight RMI server.

* Integrated JasperReports support.

* Logging provided by the Logback logging framework.


2. RELEASE INFO

Directory structure:
* "docs" contains basic documentation as well as API javadocs
* "resources" contains miscellaneous files required for running the JMinor server, the server monitor
              and the demo applications. resources/project_template contains an ant build file template
              for projects using the JMinor framework.
* "src" contains the Java source files for the framework
* "demos" contains the Java source files for the framework demos: EmpDept, Petstore, Chinook, World and SchemaBrowser
* "plugins" contains the Java sources for the framework plugins
* "build.xml" ant build file
* "build.properties" ant build properties
* "ivy.xml" ivy dependency management configuration (ant->resolve_libraries)
* "changelog.txt" the framework changelog


4. GETTING STARTED

JMinor comes with five demo applications, a small one based on the SCOTT schema found in most if not all Oracle
setups called EmpDept, a larger one called Petstore based on a schema of the same name, an application based on
a music web-store schema called Chinook, an application based on the World schema and a SchemaBrowser, which is
limited to Oracle, MySQL and H2 databases. An H2 database containing the required schemas can be generated via
the ant target 'recreate_h2_db'.

In order to run the demos you must first run the deploy_all target in the ant build file (info on how to run ant
builds can be found at ant.apache.org), which deploys the demo applications into the folder deployment along
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