/**
 * @provides org.jminor.common.db.Database
 */
module org.jminor.dbms.postgresql {
  requires org.slf4j;
  requires transitive org.jminor.common.db;

  exports org.jminor.dbms.postgresql;

  provides org.jminor.common.db.Database
          with org.jminor.dbms.postgresql.PostgreSQLDatabase;
}