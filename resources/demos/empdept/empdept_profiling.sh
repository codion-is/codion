java -Xmx128m -Djavax.net.ssl.trustStore=JMinorClientTruststore -Djminor.profiling.thinktime=1000 -Djminor.profiling.loginwait=2 -Djminor.profiling.clientcount=0 -Djminor.server.hostname=localhost -Djminor.db.type=mysql -Djava.security.policy=jminor_profiling.policy -cp lib/jminor-client.jar:lib/jminor-demos.jar:lib/jminor-common.jar:lib/log4j-1.2.15.jar:lib/jcalendar-1.3.2.jar:lib/jasperreports-3.0.0.jar:lib/jfreechart-1.0.12.jar:lib/jcommon-1.0.15.jar:lib/h2-1.1.114.jar:lib/derby.jar org.jminor.framework.demos.empdept.profiling.EmpDeptProfiling