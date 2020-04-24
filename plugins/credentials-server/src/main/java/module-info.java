/**
 * A CredentialsProvider implementation with a server component
 * @provides org.jminor.common.CredentialsProvider
 */
module org.jminor.credentials.server {
  requires org.slf4j;
  requires transitive java.rmi;
  requires transitive org.jminor.common.rmi;

  exports org.jminor.plugin.credentials.server;

  provides org.jminor.common.CredentialsProvider
          with org.jminor.plugin.credentials.server.DefaultCredentialsProvider;
}