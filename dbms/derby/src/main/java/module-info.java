/**
 * @provides org.jminor.common.db.Database
 */
module org.jminor.dbms.derby {
  requires transitive org.jminor.common.db;

  exports org.jminor.dbms.derby;

  provides org.jminor.common.db.DatabaseProvider
          with org.jminor.dbms.derby.DerbyDatabaseProvider;
}