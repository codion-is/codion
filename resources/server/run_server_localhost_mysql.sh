java -Xmx256m -Djavax.net.ssl.keyStore=JMinorServerKeystore -Djavax.net.ssl.keyStorePassword=jminor -Djminor.server.pooling.initial=scott -Djminor.db.type=mysql -Djminor.server.port=2222 -Djminor.server.db.port=80 -Djminor.db.type=mysql -Djminor.db.host=hafbrak.fiskistofa.is -Djminor.db.port=1521 -Djminor.db.sid=ran -Djava.security.policy=jminor_server.policy -cp bin/jminor-server.jar:bin/jminor-common.jar:lib/log4j-1.2.15.jar:lib/jasperreports-1.2.7.jar:lib/mysql-connector-java-5.1.5-bin.jar org.jminor.framework.server.EntityDbRemoteServer