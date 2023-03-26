/**
 * Tomcat connection pool.
 */
module is.codion.plugin.tomcat.pool {
  requires java.management;
  requires tomcat.jdbc;
  requires is.codion.common.db;

  exports is.codion.plugin.tomcat.pool;

  provides is.codion.common.db.pool.ConnectionPoolFactory
          with is.codion.plugin.tomcat.pool.TomcatConnectionPoolFactory;
}