java -Djminor.client.connectionType=remote -Djava.security.policy=jminor_demos.policy -Xmx128m -Djavax.net.ssl.trustStore=JMinorClientTruststore -Djminor.server.hostname=localhost -cp lib/jminor-client-remote.jar:lib/jminor-demos.jar:lib/jminor-common.jar:lib/log4j-1.2.15.jar:lib/jcalendar-1.3.2.jar org.jminor.framework.demos.petstore.client.ui.PetstoreAppPanel