java -Djminor.report.path=reports -Djminor.client.connectionType=local -Djminor.db.type=mysql -Djminor.db.host=localhost -Djminor.db.port=3306 -Djminor.db.sid=scott -Djava.security.policy=jminor_demos.policy -Xmx128m -cp lib/jminor-client.jar;lib/jminor-demos.jar;lib/jminor-common.jar;lib/log4j-1.2.15.jar;lib/jcalendar-1.3.2.jar;lib/jasperreports-3.0.0.jar;lib/commons-collections-2.1.jar;lib/commons-logging-1.0.2.jar;lib/itext-1.3.1.jar;lib/mysql-connector-java-5.1.5-bin.jar;lib/json.jar org.jminor.framework.demos.empdept.client.ui.EmpDeptAppPanel