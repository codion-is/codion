module org.jminor.plugin.hikari.pool {
  requires com.zaxxer.hikari;
  requires org.jminor.common.db;

  exports org.jminor.framework.plugins.hikari.pool;

  provides org.jminor.common.db.pool.ConnectionPoolProvider
          with org.jminor.framework.plugins.hikari.pool.HikariConnectionPoolProvider;
}