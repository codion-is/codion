/**
 * RMI client/server classes.
 */
module is.codion.common.rmi {
  requires org.slf4j;
  requires jdk.management;
  requires io.github.hakky54.sslcontext.kickstart;
  requires transitive java.rmi;
  requires transitive is.codion.common.core;

  exports is.codion.common.rmi.client;
  exports is.codion.common.rmi.server;
  exports is.codion.common.rmi.server.exception;

  uses is.codion.common.rmi.server.AuxiliaryServerFactory;
  uses is.codion.common.rmi.server.LoginProxy;
}