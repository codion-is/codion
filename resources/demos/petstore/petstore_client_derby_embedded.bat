java -Djminor.client.connectionType=local -Djminor.db.type=derby -Djminor.db.embedded=true -Djminor.db.host=derbydb/derby -Djava.security.policy=jminor_demos.policy -Xmx128m -cp lib/jminor-client.jar;lib/jminor-demos.jar;lib/jminor-common.jar;lib/log4j-1.2.15.jar;lib/jcalendar-1.3.2.jar;lib/jasperreports-3.0.0.jar;lib/derby.jar org.jminor.framework.demos.petstore.client.ui.PetstoreAppPanel