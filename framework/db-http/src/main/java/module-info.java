/**
 * Framework database connection classes for connections via http.
 * @provides org.jminor.framework.db.EntityConnectionProvider
 */
module org.jminor.framework.db.http {
  requires java.net.http;
  requires org.jminor.framework.db.core;

  exports org.jminor.framework.db.http;

  provides org.jminor.framework.db.EntityConnectionProvider
          with org.jminor.framework.db.http.HttpEntityConnectionProvider;
}