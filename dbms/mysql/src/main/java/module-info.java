module org.jminor.dbms.mysql {
  requires slf4j.api;
  requires transitive org.jminor.common.db;

  exports org.jminor.dbms.mysql;
}