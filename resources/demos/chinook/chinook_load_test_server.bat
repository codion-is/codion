java -Xmx256m -Djminor.configurationFile=load_test_server.config -Djava.security.policy=config/jminor_load_test_server.policy -cp config;lib/jminor-client.jar;lib/jminor-demos.jar;lib/jminor-common.jar;lib/slf4j-api-1.6.1.jar;lib/logback-core-0.9.24.jar;lib/logback-classic-0.9.24.jar;lib/jcalendar-1.3.2.jar;lib/jfreechart-1.0.12.jar;lib/jcommon-1.0.15.jar;lib/h2-1.1.114.jar;lib/jasperreports-3.0.0.jar;lib/commons-logging-1.0.2.jar;lib/commons-collections-2.1.jar org.jminor.common.server.loadtest.LoadTestServer