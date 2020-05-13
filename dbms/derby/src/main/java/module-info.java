/**
 * @provides dev.codion.common.db.database.DatabaseProvider
 */
module dev.codion.dbms.derby {
  requires transitive dev.codion.common.db;

  exports dev.codion.dbms.derby;

  provides dev.codion.common.db.database.DatabaseProvider
          with dev.codion.dbms.derby.DerbyDatabaseProvider;
}