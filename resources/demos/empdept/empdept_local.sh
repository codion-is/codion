java -Xmx128m -Djminor.configurationFile=h2_embedded.config -Djava.security.policy=config/jminor_demos.policy -cp config:lib/jminor-client.jar:lib/jminor-demos.jar:lib/jminor-common.jar:lib/slf4j-api-1.6.1.jar:lib/logback-core-0.9.24.jar:lib/logback-classic-0.9.24.jar:lib/jcalendar-1.3.2.jar:lib/h2-1.1.114.jar org.jminor.framework.demos.empdept.client.ui.EmpDeptAppPanel