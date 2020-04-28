/**
 * RMI client/server classes.
 */
module org.jminor.common.rmi {
  requires org.slf4j;
  requires transitive java.rmi;
  requires transitive org.jminor.common.core;

  exports org.jminor.common.rmi.client;
  exports org.jminor.common.rmi.server;
  exports org.jminor.common.rmi.server.exception;

  uses org.jminor.common.rmi.server.AuxiliaryServerProvider;
}