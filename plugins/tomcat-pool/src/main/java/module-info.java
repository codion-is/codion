module dev.codion.plugin.tomcat.pool {
  requires java.management;
  requires tomcat.jdbc;
  requires dev.codion.common.db;

  exports dev.codion.plugin.tomcat.pool;

  provides dev.codion.common.db.pool.ConnectionPoolProvider
          with dev.codion.plugin.tomcat.pool.TomcatConnectionPoolProvider;
}