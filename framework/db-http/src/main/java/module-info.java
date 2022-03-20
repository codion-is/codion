/**
 * Framework database connection classes for connections via http.
 * @provides is.codion.framework.db.EntityConnectionProvider
 */
module is.codion.framework.db.http {
  requires org.slf4j;
  requires java.net.http;
  requires org.apache.httpcomponents.httpcore;
  requires org.apache.httpcomponents.httpclient;
  requires com.fasterxml.jackson.core;
  requires com.fasterxml.jackson.databind;
  requires is.codion.framework.db.core;
  requires is.codion.plugin.jackson.json;

  exports is.codion.framework.db.http;

  provides is.codion.framework.db.EntityConnectionProvider.Builder
          with is.codion.framework.db.http.DefaultHttpEntityConnectionProvider.DefaultBuilder;
}