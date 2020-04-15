/**
 * Framework database connection classes for connections via http.
 * @provides org.jminor.framework.db.EntityConnectionProvider
 */
module org.jminor.framework.db.http {
  requires org.slf4j;
  requires java.net.http;
  requires org.apache.httpcomponents.httpcore;
  requires org.apache.httpcomponents.httpclient;
  requires org.jminor.framework.db.core;

  exports org.jminor.framework.db.http;

  provides org.jminor.framework.db.EntityConnectionProvider
          with org.jminor.framework.db.http.HttpEntityConnectionProvider;
}