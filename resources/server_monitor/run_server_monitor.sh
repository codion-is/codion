java -Djminor.configurationFile=server_monitor.config -Djava.security.policy=config/jminor_server_monitor.policy -cp config:lib/jminor-server-monitor.jar:lib/jminor-common.jar:lib/slf4j-api-1.6.1.jar:lib/logback-core-0.9.24.jar:lib/logback-classic-0.9.24.jar:lib/jfreechart-1.0.12.jar:lib/jcommon-1.0.15.jar:lib/jcalendar-1.3.2.jar org.jminor.framework.server.monitor.ui.MonitorPanel