/**
 * A CredentialsProvider implementation with a server component
 * @provides org.jminor.common.CredentialsProvider
 */
module org.jminor.credentials.server {
  requires transitive java.rmi;
  requires transitive org.jminor.common.remote;

  exports org.jminor.plugin.credentials.server;

  provides org.jminor.common.CredentialsProvider
          with org.jminor.plugin.credentials.server.DefaultCredentialsProvider;
}