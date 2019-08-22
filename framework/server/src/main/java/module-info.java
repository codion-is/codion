/**
 * RMI application server.
 */
module org.jminor.framework.server {
  requires org.slf4j;
  requires java.management;
  requires jdk.management;
  requires transitive org.jminor.framework.db.local;
  requires transitive org.jminor.framework.db.remote;

  exports org.jminor.framework.server;
}