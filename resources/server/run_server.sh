java -Xmx256m -Djminor.configurationFile=h2_embedded.config -Djava.security.policy=config/jminor_server.policy -cp config:lib/* org.jminor.framework.server.EntityConnectionServerAdminImpl