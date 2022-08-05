/**
 * {@link is.codion.common.db.database.Database} and {@link is.codion.common.db.database.DatabaseFactory}
 * implementations for the H2 database.
 * @provides is.codion.common.db.database.DatabaseFactory
 */
module is.codion.dbms.h2database {
  requires transitive is.codion.common.db;

  exports is.codion.dbms.h2database;

  provides is.codion.common.db.database.DatabaseFactory
          with is.codion.dbms.h2database.H2DatabaseFactory;
}