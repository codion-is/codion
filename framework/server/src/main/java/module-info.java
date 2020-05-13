/**
 * RMI application server.
 */
module dev.codion.framework.server {
  requires org.slf4j;
  requires jdk.management;
  requires transitive dev.codion.framework.db.local;
  requires transitive dev.codion.framework.db.rmi;

  exports dev.codion.framework.server;
}