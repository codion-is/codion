java -Djminor.server.hostname=localhost -Djminor.db.type=mysql -Djminor.db.host=localhost -Djminor.db.port=3306 -Djminor.db.sid=mysql -Djavax.net.ssl.trustStore=JMinorClientTruststore -Djava.security.policy=jminor_server_monitor.policy -cp lib/jminor-server-monitor.jar;lib/jminor-common.jar;lib/log4j-1.2.15.jar;lib/jasperreports-3.0.0.jar;lib/jfreechart-1.0.12.jar;lib/jcommon-1.0.15.jar;lib/jcalendar-1.3.2.jar;lib/mysql-connector-java-5.1.5-bin.jar org.jminor.framework.server.monitor.ui.MonitorPanel