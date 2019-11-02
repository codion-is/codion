module org.jminor.plugin.tomcat.pool {
  requires java.management;
  requires tomcat.jdbc;
  requires org.jminor.common.db;

  exports org.jminor.plugin.tomcat.pool;

  provides org.jminor.common.db.pool.ConnectionPoolProvider
          with org.jminor.plugin.tomcat.pool.TomcatConnectionPoolProvider;
}