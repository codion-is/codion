java -Djminor.server.hostname=localhost -Djminor.db.host=localhost -Djminor.db.type=oracle -Djminor.db.port=1521 -Djminor.db.sid=sid -Djavax.net.ssl.trustStore=JMinorClientTruststore -Djava.security.policy=jminor_server_monitor.policy -cp bin/jminor-server-monitor.jar:bin/jminor-common.jar:lib/log4j-1.2.15.jar:lib/jasperreports-3.0.0.jar:lib/jfreechart-1.0.12.jar:lib/jcommon-1.0.15.jar:lib/ojdbc14.jar org.jminor.framework.server.monitor.ui.MonitorPanel