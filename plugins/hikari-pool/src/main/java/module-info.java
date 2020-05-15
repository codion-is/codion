module is.codion.plugin.hikari.pool {
  requires com.zaxxer.hikari;
  requires is.codion.common.db;

  exports is.codion.plugin.hikari.pool;

  provides is.codion.common.db.pool.ConnectionPoolProvider
          with is.codion.plugin.hikari.pool.HikariConnectionPoolProvider;
}