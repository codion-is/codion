/**
 * Framework domain model classes.
 */
module is.codion.framework.domain {
  requires org.slf4j;
  requires transitive is.codion.common.db;

  exports is.codion.framework.domain.attribute;
  exports is.codion.framework.domain.entity;
  exports is.codion.framework.domain.entity.exception;
  exports is.codion.framework.domain.property;
  exports is.codion.framework.domain;
  exports is.codion.framework.i18n;
}