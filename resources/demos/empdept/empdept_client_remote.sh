java -Djminor.report.path=reports -Djminor.client.connectionType=remote -Djava.security.policy=jminor_demos.policy -Xmx128m -Djavax.net.ssl.trustStore=JMinorClientTruststore -Djminor.server.hostname=localhost -cp lib/jminor-client-remote.jar:lib/jminor-demos.jar:lib/jminor-common.jar:lib/jminor-plugins.jar:lib/log4j-1.2.15.jar:lib/jcalendar-1.3.2.jar:lib/jasperreports-3.0.0.jar:lib/commons-collections-2.1.jar:lib/commons-logging-1.0.2.jar:lib/itext-1.3.1.jar:lib/h2-1.1.114.jar:lib/derby.jar:lib/json.jar org.jminor.framework.demos.empdept.client.ui.EmpDeptAppPanel