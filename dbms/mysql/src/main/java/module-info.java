/**
 * {@link is.codion.common.db.database.Database} and {@link is.codion.common.db.database.DatabaseFactory}
 * implementations for the MySQL database.
 * @provides is.codion.common.db.database.DatabaseFactory
 */
module is.codion.dbms.mysql {
  requires transitive is.codion.common.db;

  exports is.codion.dbms.mysql;

  provides is.codion.common.db.database.DatabaseFactory
          with is.codion.dbms.mysql.MySQLDatabaseFactory;
}