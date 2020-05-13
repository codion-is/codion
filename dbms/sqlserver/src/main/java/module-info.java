/**
 * @provides dev.codion.common.db.database.DatabaseProvider
 */
module dev.codion.dbms.sqlserver {
  requires transitive dev.codion.common.db;

  exports dev.codion.dbms.sqlserver;

  provides dev.codion.common.db.database.DatabaseProvider
          with dev.codion.dbms.sqlserver.SQLServerDatabaseProvider;
}