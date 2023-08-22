/**
 * {@link is.codion.common.db.database.DatabaseFactory} implementation for SQL Server.<br>
 * <br>
 * {@link is.codion.dbms.sqlserver.SQLServerDatabaseFactory}
 * @provides is.codion.common.db.database.DatabaseFactory
 */
module is.codion.dbms.sqlserver {
  requires transitive is.codion.common.db;

  exports is.codion.dbms.sqlserver;

  provides is.codion.common.db.database.DatabaseFactory
          with is.codion.dbms.sqlserver.SQLServerDatabaseFactory;
}