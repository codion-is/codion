/**
 * http server classes.
 */
module dev.codion.common.http {
  requires org.slf4j;
  requires org.eclipse.jetty.server;
  requires org.eclipse.jetty.util;
  requires dev.codion.common.core;

  exports dev.codion.common.http.server;
}