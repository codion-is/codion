/**
 * @provides dev.codion.common.db.database.DatabaseProvider
 */
module dev.codion.dbms.oracle {
  requires transitive dev.codion.common.db;

  exports dev.codion.dbms.oracle;

  provides dev.codion.common.db.database.DatabaseProvider
          with dev.codion.dbms.oracle.OracleDatabaseProvider;
}