java -Xmx256m -Djavax.net.ssl.keyStore=JMinorServerKeystore -Djavax.net.ssl.keyStorePassword=jminor -Djminor.server.pooling.initial=scott,darri,james -Djminor.server.logging.status=1 -Djava.rmi.server.hostname=localhost -Djminor.db.type=mysql -Djminor.server.port=2222 -Djminor.server.db.port=80 -Djminor.db.host=localhost -Djminor.db.port=3306 -Djminor.db.sid=mysql -Djava.security.policy=jminor_server.policy -cp bin/jminor-server.jar;bin/jminor-common.jar;lib/log4j-1.2.15.jar;lib/jasperreports-3.0.0.jar;lib/mysql-connector-java-5.1.7-bin.jar org.jminor.framework.server.EntityDbRemoteServer