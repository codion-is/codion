/**
 * @provides is.codion.common.db.database.DatabaseFactory
 */
module is.codion.dbms.postgresql {
  requires transitive is.codion.common.db;

  exports is.codion.dbms.postgresql;

  provides is.codion.common.db.database.DatabaseFactory
          with is.codion.dbms.postgresql.PostgreSQLDatabaseFactory;
}