module org.jminor.framework.db.local {
  requires java.sql;
  requires slf4j.api;
  requires org.jminor.common.core;
  requires org.jminor.common.db;
  requires org.jminor.framework.db.core;
  exports org.jminor.framework.db.local;
}