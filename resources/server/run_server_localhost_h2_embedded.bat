java -Xmx256m -Djavax.net.ssl.keyStore=JMinorServerKeystore -Djavax.net.ssl.keyStorePassword=jminor -Djminor.server.pooling.initial=scott -Djminor.server.logging.status=1 -Djava.rmi.server.hostname=localhost -Djminor.db.type=h2_embedded -Djminor.server.port=2222 -Djminor.server.admin.port=4444 -Djminor.server.db.port=80 -Djminor.db.host=h2db/h2 -Djava.security.policy=jminor_server.policy -cp lib/jminor-server.jar;lib/jminor-common.jar;lib/log4j-1.2.15.jar;lib/jasperreports-3.0.0.jar;lib/h2-1.1.114.jar org.jminor.framework.server.EntityDbRemoteServer