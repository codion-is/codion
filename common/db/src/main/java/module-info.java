module org.jminor.common.db {
  requires java.sql;
  requires slf4j.api;
  requires org.jminor.common.core;
  exports org.jminor.common.db;
  exports org.jminor.common.db.condition;
  exports org.jminor.common.db.exception;
  exports org.jminor.common.db.dbms;
  exports org.jminor.common.db.reports;
  exports org.jminor.common.db.pool;
  exports org.jminor.common.db.valuemap;
  exports org.jminor.common.db.valuemap.exception;
}