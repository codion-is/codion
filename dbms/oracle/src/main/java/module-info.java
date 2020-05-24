/**
 * @provides is.codion.common.db.database.DatabaseFactory
 */
module is.codion.dbms.oracle {
  requires transitive is.codion.common.db;

  exports is.codion.dbms.oracle;

  provides is.codion.common.db.database.DatabaseFactory
          with is.codion.dbms.oracle.OracleDatabaseFactory;
}