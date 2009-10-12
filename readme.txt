JMinor Application Framework

http://jminor.org


1. INTRODUCTION

JMinor is a minimalistic Java rich client CRUD application framework based solely on J2SE components, it includes:

* Simple domain modelling in plain Java code, no XML files required.

* Integrated JUnit testing of the domain model.

* A minimal but complete JDBC abstraction layer.

* A straight forward and simple way of wiring together a rich Swing client on top of the domain model,
  all done in plain Java code, no XML configuration involved.

* Clients are run with either a local JDBC connection or served by a featherweight RMI server.

* Integrated JasperReports support.

* Logging provided by the Log4J logging framework.


2. RELEASE INFO

Release contents (~18 MB):
* "dist" contains the JMinor binary jar files
* "docs" contains basic documentation as well as API javadocs
* "lib" contains third-party libraries needed for building the framework and/or running the samples
* "projects" contains project files for the IntelliJ and Netbeans IDEs
* "resources" contains miscellaneous files required for running the JMinor server, the server monitor
              and the demo applications. resources/project_template contains an ant build file template
              for projects using the JMinor framework.
* "src" contains the Java source files for the framework
* "srcdemos" contains the Java source files for the framework demos: EmpDept, Petstore and SchemaBrowser
* "srctest" contains the JUnit test sources for the framework


3. DISTRIBUTION JAR FILES

The "dist" directory contains the following distinct jar files for use in applications.

* "jminor" (~740 KB)
- Convenient jar file containing the full framework codebase

* "jminor-client (~410 KB)
- Full client codebase, including both local and remote connection facilities
- Dependencies: jminor-common

* "jminor-client-remote (~390 KB)
- Client codebase including only remote connection facilities
- Dependencies: jminor-common

* "jminor-common" (~230 KB)
- Common codebase

* "jminor-demos.jar" (~70 KB)
- Three demo applications
- Dependencies: jminor-client, jminor-common

* "jminor_server" (~115 KB)
- RMI server codebase
- Dependencies: jminor-common

* "jminor_server_monitor" (~60 KB)
- RMI server monitor codebase
- Dependencies: jminor-common

* "jminor-api-doc" (~1.7 MB)
- API documentation


4. GETTING STARTED

JMinor comes with three demo applications, a small one based on the SCOTT schema found in most if not all Oracle
setups called EmpDept, a larger one called Petstore based on a schema of the same name and a SchemaBrowser, which
is limited to Oracle, MySQL and H2 databases. Apache Derby or H2 databases containing the required schemas are
generated via respective ant targets.

In order to run the demos you must first run the deploy_all target in the ant build file (info on how to run ant
builds can be found at ant.apache.org), which deployes the demo applications into the folder dist/deployment along
with the required databases.

dist/deployment
  '-jminor_demos          - application jar files, Derby and H2 databases, files to run the demo applications
  '-jminor_server         - server jar files, Derby and H2 databases, files to run the server
  '-jminor_server_monitor - server monitor jar files, files to run the server monitor

The demo applications can be run either with an embedded Derby or H2 database or using the JMinor RMI Server.

To run the demo applications with the RMI server you must first start the server by executing one of the run files
in the 'jminor_server' directory, with the server running you can start the remote demo client by executing one of
the ...client_remote.bat/sh run files in the 'jminor_demos' directory.


5. BUILDING THE PROJECT

The Ant build file included in the project contains standard targets for building the project, running unit tests
and packaging the compiled classes as well a targets for running the server and demo applications and creating the
required databases.