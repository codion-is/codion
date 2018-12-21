module org.jminor.framework.db.http {
  requires slf4j.api;
  requires org.jminor.common.core;
  requires org.jminor.common.db;
  requires org.jminor.framework.db.core;

  exports org.jminor.framework.db.http;

  provides org.jminor.framework.db.EntityConnectionProvider
          with org.jminor.framework.db.http.HttpEntityConnectionProvider;
}