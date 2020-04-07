/**
 * RMI client/server classes.
 */
module org.jminor.common.remote {
  requires transitive java.rmi;
  requires transitive org.jminor.common.core;

  exports org.jminor.common.remote;
  exports org.jminor.common.remote.exception;

  uses org.jminor.common.remote.Server.AuxiliaryServer;
}