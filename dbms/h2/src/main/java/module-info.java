module org.jminor.dbms.h2 {
  requires slf4j.api;
  requires transitive org.jminor.common.db;

  exports org.jminor.dbms.h2;

  provides org.jminor.common.db.Database
          with org.jminor.dbms.h2.H2Database;
}