/**
 * @provides is.codion.common.db.database.DatabaseProvider
 */
module is.codion.dbms.oracle {
  requires transitive is.codion.common.db;

  exports is.codion.dbms.oracle;

  provides is.codion.common.db.database.DatabaseProvider
          with is.codion.dbms.oracle.OracleDatabaseProvider;
}