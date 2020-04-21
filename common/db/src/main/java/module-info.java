/**
 * Classes concerned with database connectivity via JDBC.
 * @uses org.jminor.common.db.DatabaseProvider
 * @uses org.jminor.common.db.pool.ConnectionPoolProvider
 * @provides org.jminor.common.db.pool.ConnectionPoolProvider
 */
module org.jminor.common.db {
  requires transitive java.sql;
  requires transitive org.jminor.common.core;

  exports org.jminor.common.db.connection;
  exports org.jminor.common.db.database;
  exports org.jminor.common.db.exception;
  exports org.jminor.common.db.operation;
  exports org.jminor.common.db.pool;
  exports org.jminor.common.db.reports;
  exports org.jminor.common.db.result;
  exports org.jminor.common.db;

  uses org.jminor.common.db.database.DatabaseProvider;
  uses org.jminor.common.db.pool.ConnectionPoolProvider;
}