module org.jminor.dbms.postgresql {
  requires slf4j.api;
  requires transitive org.jminor.common.db;

  exports org.jminor.dbms.postgresql;
}