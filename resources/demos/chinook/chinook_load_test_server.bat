java -Xmx256m -Djminor.configurationFile=load_test_server.config -Djava.security.policy=config/jminor_load_test_server.policy -cp config;lib/* org.jminor.common.server.loadtest.LoadTestServer