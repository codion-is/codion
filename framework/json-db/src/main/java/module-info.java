/**
 * JSON.
 */
module is.codion.framework.json {
  requires transitive com.fasterxml.jackson.databind;
  requires transitive com.fasterxml.jackson.datatype.jsr310;
  requires is.codion.framework.domain;
  requires is.codion.framework.db.core;

  exports is.codion.framework.json.domain;
  exports is.codion.framework.json.db;

  uses is.codion.framework.json.domain.EntityObjectMapperFactory;
}