/**
 * {@link is.codion.common.db.database.Database} and {@link is.codion.common.db.database.DatabaseFactory}
 * implementations for the Derby database.
 * @provides is.codion.common.db.database.DatabaseFactory
 */
module is.codion.dbms.derby {
  requires transitive is.codion.common.db;

  exports is.codion.dbms.derby;

  provides is.codion.common.db.database.DatabaseFactory
          with is.codion.dbms.derby.DerbyDatabaseFactory;
}