/**
 * RMI client/server classes.
 */
module dev.codion.common.rmi {
  requires org.slf4j;
  requires transitive java.rmi;
  requires transitive dev.codion.common.core;

  exports dev.codion.common.rmi.client;
  exports dev.codion.common.rmi.server;
  exports dev.codion.common.rmi.server.exception;

  uses dev.codion.common.rmi.server.AuxiliaryServerProvider;
}