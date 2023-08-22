/**
 * RMI application server.<br>
 * <br>
 * {@link is.codion.framework.server.EntityServer}<br>
 * {@link is.codion.framework.server.EntityServerAdmin}<br>
 * {@link is.codion.framework.server.EntityServerConfiguration}<br>
 */
module is.codion.framework.server {
  requires org.slf4j;
  requires transitive is.codion.framework.db.local;
  requires transitive is.codion.framework.db.rmi;

  exports is.codion.framework.server;
}