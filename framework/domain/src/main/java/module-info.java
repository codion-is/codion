module org.jminor.framework.domain {
  requires slf4j.api;
  requires transitive org.jminor.common.db;

  exports org.jminor.framework.domain;
  exports org.jminor.framework.i18n;
}