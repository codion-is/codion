JMinor Application Framework

http://jminor.org


1. INTRODUCTION

JMinor is a minimalistic rich client CRUD application framework based solely on J2SE, it includes:

* Simple domain modelling in plain Java code, no XML files required.

* Integrated JUnit testing of the domain model.

* A minimal but complete JDBC abstraction layer.

* A straight forward and simple way of wiring together a rich Swing client on top the domain model.

* Integrated JasperReports support.

* Logging provided by the Log4J logging framework.


2. RELEASE INFO

Release contents (~13 MB):
* "dist" contains the JMinor binary jar files
* "docs" contains basic documentation as well as API javadocs
* "lib" contains third-party libraries needed for building the framework and/or running the samples
* "projects" contains project files for the IntelliJ and Netbeans IDEs
* "resources" contains miscellaneous files required for running the JMinor server, the server monitor
              and the demo applications. resources/project_template contains an ant build file template
              for projects using the JMinor framework.
* "src" contains the Java source files for the framework
* "srcdemos" contains the Java source files for the framework demos: EmpDept, PetStore and SchemaBrowser
* "srctest" contains the JUnit test sources for the framework


3. DISTRIBUTION JAR FILES

The "dist" directory contains the following distinct jar files for use in applications.

* "jminor" (~720 KB)
- Convenient jar file containing the full framework codebase

* "jminor-client (~415 KB)
- Full client codebase, including both local and remote connection facilities
- Dependencies: jminor-common

* "jminor-client-remote (~380 KB)
- Client codebase including only remote connection facilities
- Dependencies: jminor-common

* "jminor-common" (~225 KB)
- Common codebase

* "jminor-demos.jar" (~55 KB)
- Three demo applications
- Dependencies: jminor-client, jminor-common

* "jminor_server" (~120 KB)
- RMI server codebase
- Dependencies: jminor-common

* "jminor_server_monitor" (~75 KB)
- RMI server monitor codebase
- Dependencies: jminor-common


4. GETTING STARTED

Documentation on how to run the demos is available on-line at http://jminor.org