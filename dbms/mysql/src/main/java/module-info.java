module org.jminor.dbms.mysql {
  requires slf4j.api;
  requires transitive org.jminor.common.db;

  exports org.jminor.dbms.mysql;

  provides org.jminor.common.db.Database
          with org.jminor.dbms.mysql.MySQLDatabase;
}