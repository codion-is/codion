/**
 * @provides is.codion.common.db.database.DatabaseProvider
 */
module is.codion.dbms.sqlserver {
  requires transitive is.codion.common.db;

  exports is.codion.dbms.sqlserver;

  provides is.codion.common.db.database.DatabaseProvider
          with is.codion.dbms.sqlserver.SQLServerDatabaseProvider;
}