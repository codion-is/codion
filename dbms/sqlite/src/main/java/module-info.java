module org.jminor.dbms.sqlite {
  requires slf4j.api;
  requires transitive org.jminor.common.db;

  exports org.jminor.dbms.sqlite;
}