/**
 * @provides is.codion.common.db.database.Database
 */
module is.codion.dbms.mariadb {
  requires transitive is.codion.common.db;

  exports is.codion.dbms.mariadb;

  provides is.codion.common.db.database.DatabaseProvider
          with is.codion.dbms.mariadb.MariaDbDatabaseProvider;
}