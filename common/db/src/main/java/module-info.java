/**
 * Classes concerned with database connectivity via JDBC.
 * @uses org.jminor.common.db.DatabaseProvider
 * @uses org.jminor.common.db.pool.ConnectionPoolProvider
 * @provides org.jminor.common.db.pool.ConnectionPoolProvider
 */
module org.jminor.common.db {
  requires transitive java.sql;
  requires transitive org.jminor.common.core;

  exports org.jminor.common.db;
  exports org.jminor.common.db.exception;
  exports org.jminor.common.db.reports;
  exports org.jminor.common.db.operation;
  exports org.jminor.common.db.pool;

  uses org.jminor.common.db.DatabaseProvider;
  uses org.jminor.common.db.pool.ConnectionPoolProvider;
}