/**
 * A CredentialsProvider implementation with a server component, for supplying single sign on for applications running on localhost.
 * @provides is.codion.common.model.credentials.CredentialsProvider
 * @uses is.codion.common.model.credentials.CredentialsProvider
 */
module is.codion.plugin.credentials.server {
  requires org.slf4j;
  requires transitive java.rmi;
  requires transitive is.codion.common.model;
  requires transitive is.codion.common.rmi;

  exports is.codion.plugin.credentials.server;

  provides is.codion.common.model.credentials.CredentialsProvider
          with is.codion.plugin.credentials.server.DefaultCredentialsProvider;
}