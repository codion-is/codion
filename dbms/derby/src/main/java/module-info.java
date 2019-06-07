module org.jminor.dbms.derby {
  requires slf4j.api;
  requires transitive org.jminor.common.db;

  exports org.jminor.dbms.derby;
}