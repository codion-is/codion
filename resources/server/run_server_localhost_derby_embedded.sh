java -Xmx256m -Djavax.net.ssl.keyStore=JMinorServerKeystore -Djavax.net.ssl.keyStorePassword=jminor -Djminor.server.pooling.initial=scott -Djminor.server.logging.status=1 -Djava.rmi.server.hostname=localhost -Djminor.db.type=derby -Djminor.db.embedded=true -Djminor.server.port=2222 -Djminor.server.admin.port=4444 -Djminor.server.db.port=3333 -Djminor.db.host=derbydb/derby -Djava.security.policy=jminor_server.policy -cp lib/jminor-server.jar:lib/jminor-common.jar:lib/log4j-1.2.15.jar:lib/jasperreports-3.0.0.jar:lib/commons-logging-1.0.2.jar:lib/commons-collections-2.1.jar:lib/derby.jar org.jminor.framework.server.EntityDbRemoteServerAdmin