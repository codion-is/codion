/**
 * @provides org.jminor.common.db.Database
 */
module org.jminor.dbms.h2database {
  requires slf4j.api;
  requires transitive org.jminor.common.db;

  exports org.jminor.dbms.h2database;

  provides org.jminor.common.db.Database
          with org.jminor.dbms.h2database.H2Database;
}