/**
 * @provides is.codion.common.db.database.DatabaseFactory
 */
module is.codion.dbms.sqlite {
  requires transitive is.codion.common.db;

  exports is.codion.dbms.sqlite;

  provides is.codion.common.db.database.DatabaseFactory
          with is.codion.dbms.sqlite.SQLiteDatabaseFactory;
}