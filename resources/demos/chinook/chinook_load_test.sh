java -Xmx128m -Djminor.configurationFile=load_test.config -Djava.security.policy=config/jminor_load_test.policy -cp config:lib/jminor-client.jar:lib/jminor-demos.jar:lib/jminor-common.jar:lib/slf4j-api-1.6.1.jar:lib/logback-core-0.9.24.jar:lib/logback-classic-0.9.24.jar:lib/jcalendar-1.3.2.jar:lib/jfreechart-1.0.12.jar:lib/jcommon-1.0.15.jar:lib/h2-1.1.114.jar org.jminor.framework.demos.chinook.testing.ChinookLoadTest