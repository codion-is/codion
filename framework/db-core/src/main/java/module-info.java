module org.jminor.framework.db.core {
  requires java.sql;
  requires slf4j.api;
  requires org.jminor.common.core;
  requires org.jminor.common.db;
  requires org.jminor.common.server;
  exports org.jminor.framework.db;
  exports org.jminor.framework.db.condition;
  exports org.jminor.framework.domain;
  exports org.jminor.framework.i18n;
}