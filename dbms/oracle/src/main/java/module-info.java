/**
 * @provides org.jminor.common.db.database.DatabaseProvider
 */
module org.jminor.dbms.oracle {
  requires transitive org.jminor.common.db;

  exports org.jminor.dbms.oracle;

  provides org.jminor.common.db.database.DatabaseProvider
          with org.jminor.dbms.oracle.OracleDatabaseProvider;
}