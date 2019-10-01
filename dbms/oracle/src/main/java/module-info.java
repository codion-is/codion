/**
 * @provides org.jminor.common.db.Database
 */
module org.jminor.dbms.oracle {
  requires transitive org.jminor.common.db;

  exports org.jminor.dbms.oracle;

  provides org.jminor.common.db.Database
          with org.jminor.dbms.oracle.OracleDatabase;
}