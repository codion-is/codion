java -Xmx128m -Djminor.configurationFile=mysql.config -Djava.security.policy=jminor_demos.policy -cp config:lib/jminor-client.jar:lib/jminor-demos.jar:lib/jminor-common.jar:lib/log4j-1.2.15.jar:lib/jcalendar-1.3.2.jar:lib/h2-1.1.114.jar org.jminor.framework.demos.schemabrowser.client.ui.SchemaBrowserAppPanel