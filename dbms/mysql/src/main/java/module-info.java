/**
 * @provides org.jminor.common.db.Database
 */
module org.jminor.dbms.mysql {
  requires org.slf4j;
  requires transitive org.jminor.common.db;

  exports org.jminor.dbms.mysql;

  provides org.jminor.common.db.Database
          with org.jminor.dbms.mysql.MySQLDatabase;
}