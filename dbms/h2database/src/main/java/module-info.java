/**
 * @provides dev.codion.common.db.database.DatabaseProvider
 */
module dev.codion.dbms.h2database {
  requires transitive dev.codion.common.db;

  exports dev.codion.dbms.h2database;

  provides dev.codion.common.db.database.DatabaseProvider
          with dev.codion.dbms.h2database.H2DatabaseProvider;
}