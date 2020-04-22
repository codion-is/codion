/**
 * @provides org.jminor.common.db.database.Database
 */
module org.jminor.dbms.mariadb {
  requires transitive org.jminor.common.db;

  exports org.jminor.dbms.mariadb;

  provides org.jminor.common.db.database.DatabaseProvider
          with org.jminor.dbms.mariadb.MariaDbDatabaseProvider;
}