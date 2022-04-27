/**
 * A CredentialsProvider implementation with a server component
 * @provides is.codion.common.credentials.CredentialsProvider
 */
module is.codion.plugin.credentials.server {
  requires org.slf4j;
  requires transitive java.rmi;
  requires transitive is.codion.common.rmi;

  exports is.codion.plugin.credentials.server;

  provides is.codion.common.credentials.CredentialsProvider
          with is.codion.plugin.credentials.server.DefaultCredentialsProvider;
}