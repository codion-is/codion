/**
 * @provides org.jminor.common.db.database.DatabaseProvider
 */
module org.jminor.dbms.postgresql {
  requires transitive org.jminor.common.db;

  exports org.jminor.dbms.postgresql;

  provides org.jminor.common.db.database.DatabaseProvider
          with org.jminor.dbms.postgresql.PostgreSQLDatabaseProvider;
}