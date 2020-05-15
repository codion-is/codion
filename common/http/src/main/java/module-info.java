/**
 * http server classes.
 */
module is.codion.common.http {
  requires org.slf4j;
  requires org.eclipse.jetty.server;
  requires org.eclipse.jetty.util;
  requires is.codion.common.core;

  exports is.codion.common.http.server;
}