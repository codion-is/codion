module dev.codion.plugin.hikari.pool {
  requires com.zaxxer.hikari;
  requires dev.codion.common.db;

  exports dev.codion.plugin.hikari.pool;

  provides dev.codion.common.db.pool.ConnectionPoolProvider
          with dev.codion.plugin.hikari.pool.HikariConnectionPoolProvider;
}