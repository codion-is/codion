/**
 * JSON Db.
 */
module is.codion.framework.json.db {
  requires transitive com.fasterxml.jackson.databind;
  requires transitive com.fasterxml.jackson.datatype.jsr310;
  requires is.codion.framework.domain;
  requires is.codion.framework.db.core;
  requires is.codion.framework.json.domain;

  exports is.codion.framework.json.db;
}