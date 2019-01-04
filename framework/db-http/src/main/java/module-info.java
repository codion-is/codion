module org.jminor.framework.db.http {
  requires slf4j.api;
  requires org.apache.httpcomponents.httpclient;
  requires org.apache.httpcomponents.httpcore;
  requires org.jminor.framework.db.core;

  exports org.jminor.framework.db.http;

  provides org.jminor.framework.db.EntityConnectionProvider
          with org.jminor.framework.db.http.HttpEntityConnectionProvider;
}