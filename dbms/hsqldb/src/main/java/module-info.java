/**
 * @provides dev.codion.common.db.database.DatabaseProvider
 */
module dev.codion.dbms.hsqldb {
  requires transitive dev.codion.common.db;

  exports dev.codion.dbms.hsqldb;

  provides dev.codion.common.db.database.DatabaseProvider
          with dev.codion.dbms.hsqldb.HSQLDatabaseProvider;
}