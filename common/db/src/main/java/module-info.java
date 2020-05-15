/**
 * Classes concerned with database connectivity via JDBC.
 * @uses is.codion.common.db.database.DatabaseProvider
 * @uses is.codion.common.db.pool.ConnectionPoolProvider
 * @provides is.codion.common.db.pool.ConnectionPoolProvider
 */
module is.codion.common.db {
  requires transitive java.sql;
  requires transitive is.codion.common.core;

  exports is.codion.common.db.connection;
  exports is.codion.common.db.database;
  exports is.codion.common.db.exception;
  exports is.codion.common.db.operation;
  exports is.codion.common.db.pool;
  exports is.codion.common.db.reports;
  exports is.codion.common.db.result;
  exports is.codion.common.db;

  uses is.codion.common.db.database.DatabaseProvider;
  uses is.codion.common.db.pool.ConnectionPoolProvider;
}