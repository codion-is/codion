/**
 * @provides org.jminor.common.db.Database
 */
module org.jminor.dbms.hsqldb {
  requires org.slf4j;
  requires transitive org.jminor.common.db;

  exports org.jminor.dbms.hsqldb;

  provides org.jminor.common.db.Database
          with org.jminor.dbms.hsqldb.HSQLDatabase;
}