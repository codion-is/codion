/**
 * @provides is.codion.common.db.database.DatabaseProvider
 */
module is.codion.dbms.postgresql {
  requires transitive is.codion.common.db;

  exports is.codion.dbms.postgresql;

  provides is.codion.common.db.database.DatabaseProvider
          with is.codion.dbms.postgresql.PostgreSQLDatabaseProvider;
}