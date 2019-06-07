module org.jminor.dbms.sqlserver {
  requires slf4j.api;
  requires transitive org.jminor.common.db;

  exports org.jminor.dbms.sqlserver;

  provides org.jminor.common.db.Database
          with org.jminor.dbms.sqlserver.SQLServerDatabase;
}