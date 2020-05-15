/**
 * @provides is.codion.common.db.database.DatabaseProvider
 */
module is.codion.dbms.mysql {
  requires transitive is.codion.common.db;

  exports is.codion.dbms.mysql;

  provides is.codion.common.db.database.DatabaseProvider
          with is.codion.dbms.mysql.MySQLDatabaseProvider;
}