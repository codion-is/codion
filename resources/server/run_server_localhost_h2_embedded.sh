java -Xmx256m -Djminor.db.useOptimisticLocking=true -Djavax.net.ssl.keyStore=JMinorServerKeystore -Djavax.net.ssl.keyStorePassword=jminor -Djminor.server.pooling.initial=scott -Djminor.server.logging.status=true -Djava.rmi.server.hostname=localhost -Djminor.db.type=h2 -Djminor.db.embedded=true -Djminor.server.port=2223 -Djminor.server.admin.port=4445 -Djminor.server.db.port=3334 -Djminor.db.host=h2db/h2 -Djava.security.policy=jminor_server.policy -cp lib/jminor-server.jar:lib/jminor-common.jar:lib/log4j-1.2.15.jar:lib/jasperreports-3.0.0.jar:lib/commons-logging-1.0.2.jar:lib/commons-collections-2.1.jar:lib/h2-1.1.114.jar org.jminor.framework.server.EntityDbRemoteServerAdmin