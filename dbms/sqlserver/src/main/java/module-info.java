/**
 * {@link is.codion.common.db.database.Database} and {@link is.codion.common.db.database.DatabaseFactory}
 * implementations for the SQL Server database.
 * @provides is.codion.common.db.database.DatabaseFactory
 */
module is.codion.dbms.sqlserver {
  requires transitive is.codion.common.db;

  exports is.codion.dbms.sqlserver;

  provides is.codion.common.db.database.DatabaseFactory
          with is.codion.dbms.sqlserver.SQLServerDatabaseFactory;
}