/**
 * @provides org.jminor.common.db.Database
 */
module org.jminor.dbms.hsqldb {
  requires slf4j.api;
  requires transitive org.jminor.common.db;

  exports org.jminor.dbms.hsqldb;

  provides org.jminor.common.db.Database
          with org.jminor.dbms.hsqldb.HSQLDatabase;
}