/**
 * Classes concerned with database connectivity via JDBC.
 * @uses dev.codion.common.db.database.DatabaseProvider
 * @uses dev.codion.common.db.pool.ConnectionPoolProvider
 * @provides dev.codion.common.db.pool.ConnectionPoolProvider
 */
module dev.codion.common.db {
  requires transitive java.sql;
  requires transitive dev.codion.common.core;

  exports dev.codion.common.db.connection;
  exports dev.codion.common.db.database;
  exports dev.codion.common.db.exception;
  exports dev.codion.common.db.operation;
  exports dev.codion.common.db.pool;
  exports dev.codion.common.db.reports;
  exports dev.codion.common.db.result;
  exports dev.codion.common.db;

  uses dev.codion.common.db.database.DatabaseProvider;
  uses dev.codion.common.db.pool.ConnectionPoolProvider;
}