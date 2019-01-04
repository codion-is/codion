module org.jminor.framework.db.core {
  requires slf4j.api;
  requires transitive org.jminor.common.db;

  exports org.jminor.framework.db;
  exports org.jminor.framework.db.condition;
  exports org.jminor.framework.domain;
  exports org.jminor.framework.i18n;

  uses org.jminor.framework.db.EntityConnectionProvider;
}