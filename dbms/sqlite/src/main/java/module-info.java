/**
 * @provides dev.codion.common.db.database.DatabaseProvider
 */
module dev.codion.dbms.sqlite {
  requires transitive dev.codion.common.db;

  exports dev.codion.dbms.sqlite;

  provides dev.codion.common.db.database.DatabaseProvider
          with dev.codion.dbms.sqlite.SQLiteDatabaseProvider;
}