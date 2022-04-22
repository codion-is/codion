/**
 * @provides is.codion.common.db.database.DatabaseFactory
 */
module is.codion.dbms.db2 {
  requires transitive is.codion.common.db;

  exports is.codion.dbms.db2;

  provides is.codion.common.db.database.DatabaseFactory
          with is.codion.dbms.db2.Db2DatabaseFactory;
}