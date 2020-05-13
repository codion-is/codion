/**
 * Framework database connection classes for connections via http.
 * @provides dev.codion.framework.db.EntityConnectionProvider
 */
module dev.codion.framework.db.http {
  requires org.slf4j;
  requires java.net.http;
  requires org.apache.httpcomponents.httpcore;
  requires org.apache.httpcomponents.httpclient;
  requires dev.codion.framework.db.core;

  exports dev.codion.framework.db.http;

  provides dev.codion.framework.db.EntityConnectionProvider
          with dev.codion.framework.db.http.HttpEntityConnectionProvider;
}