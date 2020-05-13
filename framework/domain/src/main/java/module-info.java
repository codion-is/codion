/**
 * Framework domain model classes.
 */
module dev.codion.framework.domain {
  requires org.slf4j;
  requires transitive dev.codion.common.db;

  exports dev.codion.framework.domain.entity;
  exports dev.codion.framework.domain.entity.exception;
  exports dev.codion.framework.domain.property;
  exports dev.codion.framework.domain;
  exports dev.codion.framework.i18n;
}