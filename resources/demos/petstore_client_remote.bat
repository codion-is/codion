java -Djminor.client.connection.type=remote -Djava.security.policy=jminor_demos.policy -Xmx128m -Djavax.net.ssl.trustStore=JMinorClientTruststore -Djminor.server.hostname=localhost -cp bin/jminor-client-remote.jar;bin/jminor-demos.jar;bin/jminor-common.jar;lib/log4j-1.2.15.jar;lib/jasperreports-1.2.7.jar org.jminor.framework.demos.petstore.client.ui.PetstoreAppPanel