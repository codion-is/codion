/**
 * {@link is.codion.common.db.database.DatabaseFactory} implementation for Db2.<br>
 * <br>
 * {@link is.codion.dbms.db2database.Db2DatabaseFactory}
 * @provides is.codion.common.db.database.DatabaseFactory
 */
module is.codion.dbms.db2database {
  requires transitive is.codion.common.db;

  exports is.codion.dbms.db2database;

  provides is.codion.common.db.database.DatabaseFactory
          with is.codion.dbms.db2database.Db2DatabaseFactory;
}