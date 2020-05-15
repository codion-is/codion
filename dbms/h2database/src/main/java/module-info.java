/**
 * @provides is.codion.common.db.database.DatabaseProvider
 */
module is.codion.dbms.h2database {
  requires transitive is.codion.common.db;

  exports is.codion.dbms.h2database;

  provides is.codion.common.db.database.DatabaseProvider
          with is.codion.dbms.h2database.H2DatabaseProvider;
}