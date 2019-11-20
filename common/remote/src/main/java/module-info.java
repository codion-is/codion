/**
 * RMI client/server classes.
 * @provides org.jminor.common.CredentialsProvider
 */
module org.jminor.common.remote {
  requires transitive java.rmi;
  requires transitive org.jminor.common.core;

  exports org.jminor.common.remote;
  exports org.jminor.common.remote.exception;
}