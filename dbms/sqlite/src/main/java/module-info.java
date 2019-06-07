module org.jminor.dbms.sqlite {
  requires slf4j.api;
  requires transitive org.jminor.common.db;

  exports org.jminor.dbms.sqlite;

  provides org.jminor.common.db.Database
          with org.jminor.dbms.sqlite.SQLiteDatabase;
}