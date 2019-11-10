/**
 * Framework domain model classes.
 */
module org.jminor.framework.domain {
  requires transitive org.jminor.common.db;

  exports org.jminor.framework.domain.property;
  exports org.jminor.framework.domain;
  exports org.jminor.framework.i18n;
}