java -Djminor.client.connection.type=local -Djminor.db.type=mysql -Djminor.db.host=localhost -Djminor.db.port=3306 -Djminor.db.sid=scott -Djava.security.policy=jminor_demos.policy -Xmx128m -cp bin/jminor-client.jar;bin/jminor-demos.jar;bin/jminor-common.jar;lib/log4j-1.2.15.jar;lib/jasperreports-3.0.0.jar;lib/mysql-connector-java-5.1.5-bin.jar org.jminor.framework.demos.empdept.client.ui.EmpDeptAppPanel