/**
 * JSON Domain.
 */
module is.codion.framework.json.domain {
  requires transitive com.fasterxml.jackson.databind;
  requires transitive com.fasterxml.jackson.datatype.jsr310;
  requires is.codion.framework.domain;

  exports is.codion.framework.json.domain;

  uses is.codion.framework.json.domain.EntityObjectMapperFactory;
}