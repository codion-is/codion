/**
 * JSON serialization for domain related classes.<br>
 * <br>
 * {@link is.codion.framework.json.domain.EntityObjectMapper}<br>
 * {@link is.codion.framework.json.domain.EntityObjectMapperFactory}<br>
 */
module is.codion.framework.json.domain {
  requires transitive com.fasterxml.jackson.databind;
  requires transitive com.fasterxml.jackson.datatype.jsr310;
  requires is.codion.framework.domain;

  exports is.codion.framework.json.domain;

  uses is.codion.framework.json.domain.EntityObjectMapperFactory;
}