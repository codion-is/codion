java -Djminor.client.connection.type=local -Djminor.db.type=mysql -Djminor.db.host=localhost -Djminor.db.port=3306 -Djminor.db.sid=scott -Djava.security.policy=jminor_demos.policy -Xmx128m -cp jminor-client.jar;jminor-demos.jar;jminor-common.jar;lib/log4j-1.2.15.jar;lib/jasperreports-1.2.7.jar;lib/mysql-connector-java-5.1.5-bin.jar org.jminor.framework.demos.schemabrowser.client.ui.SchemaBrowserAppPanel