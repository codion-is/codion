/**
 * @provides is.codion.common.db.database.DatabaseFactory
 */
module is.codion.dbms.mariadb {
  requires transitive is.codion.common.db;

  exports is.codion.dbms.mariadb;

  provides is.codion.common.db.database.DatabaseFactory
          with is.codion.dbms.mariadb.MariaDBDatabaseFactory;
}