/**
 * @provides dev.codion.common.db.database.DatabaseProvider
 */
module dev.codion.dbms.mysql {
  requires transitive dev.codion.common.db;

  exports dev.codion.dbms.mysql;

  provides dev.codion.common.db.database.DatabaseProvider
          with dev.codion.dbms.mysql.MySQLDatabaseProvider;
}