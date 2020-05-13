/**
 * @provides dev.codion.common.db.database.DatabaseProvider
 */
module dev.codion.dbms.postgresql {
  requires transitive dev.codion.common.db;

  exports dev.codion.dbms.postgresql;

  provides dev.codion.common.db.database.DatabaseProvider
          with dev.codion.dbms.postgresql.PostgreSQLDatabaseProvider;
}