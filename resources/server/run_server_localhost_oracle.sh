java -Xmx256m -Djminor.server.domain.jars=lib/jminor-demos.jar -Djminor.server.domain.classes=org.jminor.framework.demos.empdept.domain.EmpDept,org.jminor.framework.demos.petstore.domain.Petstore -Djminor.db.useOptimisticLocking=true -Djavax.net.ssl.keyStore=JMinorServerKeystore -Djavax.net.ssl.keyStorePassword=jminor -Djminor.server.pooling.initial=scott -Djava.rmi.server.hostname=localhost -Djminor.db.type=oracle -Djminor.server.port=2222 -Djminor.server.admin.port=4444 -Djminor.server.db.port=3333 -Djminor.db.host=stofupallur.fiskistofa.is -Djminor.db.port=1521 -Djminor.db.sid=ran -Djava.security.policy=jminor_server.policy -cp lib/jminor-server.jar:lib/jminor-common.jar:lib/log4j-1.2.15.jar:lib/jasperreports-3.0.0.jar:lib/commons-logging-1.0.2.jar:lib/commons-collections-2.1.jar:lib/ojdbc14.jar:lib/json.jar org.jminor.framework.server.EntityDbRemoteServerAdmin