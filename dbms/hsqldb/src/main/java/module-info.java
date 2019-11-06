/**
 * @provides org.jminor.common.db.Database
 */
module org.jminor.dbms.hsqldb {
  requires transitive org.jminor.common.db;

  exports org.jminor.dbms.hsqldb;

  provides org.jminor.common.db.DatabaseProvider
          with org.jminor.dbms.hsqldb.HSQLDatabaseProvider;
}