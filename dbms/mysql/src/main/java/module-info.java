/**
 * @provides org.jminor.common.db.Database
 */
module org.jminor.dbms.mysql {
  requires transitive org.jminor.common.db;

  exports org.jminor.dbms.mysql;

  provides org.jminor.common.db.DatabaseProvider
          with org.jminor.dbms.mysql.MySQLDatabaseProvider;
}