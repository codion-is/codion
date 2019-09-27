###### JMinor Application Framework

#### Introduction
Way back in 2004 I was assigned the task of writing a [CRUD](https://en.wikipedia.org/wiki/Create,_read,_update_and_delete) application for a small department in the research institute where I work as a programmer. Two or three people would use this application to enter research data into a database. I started the project using a rich client framework recommended by a collegue but after a week of what felt like banging my head against a rock I decided to scrap what I had and start over, without a framework, using just [Java SE](https://en.wikipedia.org/wiki/Java_Platform,_Standard_Edition) components, [JDBC](https://en.wikipedia.org/wiki/Java_Database_Connectivity), [Swing](https://en.wikipedia.org/wiki/Swing_(Java)) and [RMI](https://en.wikipedia.org/wiki/Java_remote_method_invocation).

Half way through the project I managed to refactor out a few generic components and thus began the development of the JMinor framework, which has been my hobby ever since. I've been using and developing the framework for the last 15 years, always with the aim of eventually open sourcing it.

#### Features
* JMinor is a full stack framework, modular and minimalistic while providing all the fundamental building blocks required for a rich CRUD client application.
* A JMinor application contains no XML configuration and no annotations, everything from the domain model to the UI is expressed in plain Java code.
* Includes a light-weight RMI/HTTP server to use when direct access to the database is not available.
* A JMinor application is flexible and extendible.