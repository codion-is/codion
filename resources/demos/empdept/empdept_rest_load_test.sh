java -Xmx512m -Djminor.configurationFile=rest_load_test.config -Djava.security.policy=config/jminor_load_test.policy -cp config:lib/* org.jminor.framework.demos.empdept.rest.EmpDeptRESTLoadTest &