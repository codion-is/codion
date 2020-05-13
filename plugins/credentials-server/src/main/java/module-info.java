/**
 * A CredentialsProvider implementation with a server component
 * @provides dev.codion.common.CredentialsProvider
 */
module dev.codion.credentials.server {
  requires org.slf4j;
  requires transitive java.rmi;
  requires transitive dev.codion.common.rmi;

  exports dev.codion.plugin.credentials.server;

  provides dev.codion.common.CredentialsProvider
          with dev.codion.plugin.credentials.server.DefaultCredentialsProvider;
}