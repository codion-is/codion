module org.jminor.common.db {
  requires slf4j.api;
  requires transitive java.sql;
  requires transitive org.jminor.common.core;

  exports org.jminor.common.db;
  exports org.jminor.common.db.condition;
  exports org.jminor.common.db.exception;
  exports org.jminor.common.db.reports;
  exports org.jminor.common.db.pool;
  exports org.jminor.common.db.valuemap;
  exports org.jminor.common.db.valuemap.exception;

  provides org.jminor.common.db.pool.ConnectionPoolProvider
          with org.jminor.common.db.pool.DefaultConnectionPoolProvider;

  uses org.jminor.common.db.pool.ConnectionPoolProvider;
}