/**
 * @provides org.jminor.common.db.Database
 */
module org.jminor.dbms.sqlserver {
  requires org.slf4j;
  requires transitive org.jminor.common.db;

  exports org.jminor.dbms.sqlserver;

  provides org.jminor.common.db.Database
          with org.jminor.dbms.sqlserver.SQLServerDatabase;
}