/**
 * @provides dev.codion.common.db.database.Database
 */
module dev.codion.dbms.mariadb {
  requires transitive dev.codion.common.db;

  exports dev.codion.dbms.mariadb;

  provides dev.codion.common.db.database.DatabaseProvider
          with dev.codion.dbms.mariadb.MariaDbDatabaseProvider;
}