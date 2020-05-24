/**
 * @provides is.codion.common.db.database.DatabaseFactory
 */
module is.codion.dbms.hsqldb {
  requires transitive is.codion.common.db;

  exports is.codion.dbms.hsqldb;

  provides is.codion.common.db.database.DatabaseFactory
          with is.codion.dbms.hsqldb.HSQLDatabaseFactory;
}