module org.jminor.framework.plugins.tomcat.pool {
  requires java.sql;
  requires tomcat.jdbc;
  requires org.jminor.common.core;
  requires org.jminor.common.db;

  exports org.jminor.framework.plugins.tomcat.pool;
}