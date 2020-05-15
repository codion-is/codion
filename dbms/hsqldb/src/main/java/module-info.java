/**
 * @provides is.codion.common.db.database.DatabaseProvider
 */
module is.codion.dbms.hsqldb {
  requires transitive is.codion.common.db;

  exports is.codion.dbms.hsqldb;

  provides is.codion.common.db.database.DatabaseProvider
          with is.codion.dbms.hsqldb.HSQLDatabaseProvider;
}