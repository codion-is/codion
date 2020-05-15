/**
 * Framework database connection classes for connections via http.
 * @provides is.codion.framework.db.EntityConnectionProvider
 */
module is.codion.framework.db.http {
  requires org.slf4j;
  requires java.net.http;
  requires org.apache.httpcomponents.httpcore;
  requires org.apache.httpcomponents.httpclient;
  requires is.codion.framework.db.core;

  exports is.codion.framework.db.http;

  provides is.codion.framework.db.EntityConnectionProvider
          with is.codion.framework.db.http.HttpEntityConnectionProvider;
}