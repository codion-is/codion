/**
 * RMI client/server classes.
 */
module org.jminor.common.remote {
  requires org.slf4j;
  requires transitive java.rmi;
  requires transitive org.jminor.common.core;

  exports org.jminor.common.remote.client;
  exports org.jminor.common.remote.server;
  exports org.jminor.common.remote.server.exception;

  uses org.jminor.common.remote.server.Server.AuxiliaryServer;
}