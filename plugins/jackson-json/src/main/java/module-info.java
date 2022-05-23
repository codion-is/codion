module is.codion.plugin.jackson.json {
  requires transitive com.fasterxml.jackson.databind;
  requires transitive com.fasterxml.jackson.datatype.jsr310;
  requires is.codion.framework.domain;
  requires is.codion.framework.db.core;

  exports is.codion.plugin.jackson.json.domain;
  exports is.codion.plugin.jackson.json.db;

  uses is.codion.plugin.jackson.json.domain.EntityObjectMapperFactory;
}