module org.jminor.framework.plugins.hikari.pool {
  requires java.sql;
  requires com.zaxxer.hikari;
  requires org.jminor.common.core;
  requires org.jminor.common.db;

  exports org.jminor.framework.plugins.hikari.pool;
}