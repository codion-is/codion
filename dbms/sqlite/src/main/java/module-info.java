/**
 * {@link is.codion.common.db.database.Database} and {@link is.codion.common.db.database.DatabaseFactory}
 * implementations for the SQLite database.
 * @provides is.codion.common.db.database.DatabaseFactory
 */
module is.codion.dbms.sqlite {
  requires transitive is.codion.common.db;

  exports is.codion.dbms.sqlite;

  provides is.codion.common.db.database.DatabaseFactory
          with is.codion.dbms.sqlite.SQLiteDatabaseFactory;
}