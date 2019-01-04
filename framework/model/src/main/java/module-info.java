module org.jminor.framework.model {
  requires slf4j.api;
  requires transitive org.jminor.common.model;
  requires transitive org.jminor.framework.db.core;

  exports org.jminor.framework.model;
}