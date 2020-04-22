/**
 * @provides org.jminor.common.db.database.DatabaseProvider
 */
module org.jminor.dbms.sqlserver {
  requires transitive org.jminor.common.db;

  exports org.jminor.dbms.sqlserver;

  provides org.jminor.common.db.database.DatabaseProvider
          with org.jminor.dbms.sqlserver.SQLServerDatabaseProvider;
}