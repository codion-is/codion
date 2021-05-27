/**
 * Classes concerned with database connectivity via JDBC.
 * @uses is.codion.common.db.database.DatabaseFactory
 * @uses is.codion.common.db.pool.ConnectionPoolFactory
 * @provides is.codion.common.db.pool.ConnectionPoolFactory
 */
module is.codion.common.db {
  requires transitive java.sql;
  requires transitive is.codion.common.core;

  exports is.codion.common.db.connection;
  exports is.codion.common.db.database;
  exports is.codion.common.db.exception;
  exports is.codion.common.db.operation;
  exports is.codion.common.db.pool;
  exports is.codion.common.db.report;
  exports is.codion.common.db.result;

  uses is.codion.common.db.database.DatabaseFactory;
  uses is.codion.common.db.pool.ConnectionPoolFactory;
}