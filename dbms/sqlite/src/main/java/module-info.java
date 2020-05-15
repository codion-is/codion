/**
 * @provides is.codion.common.db.database.DatabaseProvider
 */
module is.codion.dbms.sqlite {
  requires transitive is.codion.common.db;

  exports is.codion.dbms.sqlite;

  provides is.codion.common.db.database.DatabaseProvider
          with is.codion.dbms.sqlite.SQLiteDatabaseProvider;
}