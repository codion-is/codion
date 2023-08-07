/**
 * RMI application server.
 */
module is.codion.framework.server {
  requires org.slf4j;
  requires transitive is.codion.framework.db.local;
  requires transitive is.codion.framework.db.rmi;

  exports is.codion.framework.server;
}