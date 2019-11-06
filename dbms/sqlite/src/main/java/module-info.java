/**
 * @provides org.jminor.common.db.Database
 */
module org.jminor.dbms.sqlite {
  requires transitive org.jminor.common.db;

  exports org.jminor.dbms.sqlite;

  provides org.jminor.common.db.DatabaseProvider
          with org.jminor.dbms.sqlite.SQLiteDatabaseProvider;
}