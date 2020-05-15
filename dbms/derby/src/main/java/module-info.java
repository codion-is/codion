/**
 * @provides is.codion.common.db.database.DatabaseProvider
 */
module is.codion.dbms.derby {
  requires transitive is.codion.common.db;

  exports is.codion.dbms.derby;

  provides is.codion.common.db.database.DatabaseProvider
          with is.codion.dbms.derby.DerbyDatabaseProvider;
}