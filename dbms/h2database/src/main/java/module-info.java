/**
 * {@link is.codion.common.db.database.DatabaseFactory} implementation for H2.<br>
 * <br>
 * {@link is.codion.dbms.h2database.H2DatabaseFactory}
 * @provides is.codion.common.db.database.DatabaseFactory
 */
module is.codion.dbms.h2database {
  requires transitive is.codion.common.db;

  exports is.codion.dbms.h2database;

  provides is.codion.common.db.database.DatabaseFactory
          with is.codion.dbms.h2database.H2DatabaseFactory;
}