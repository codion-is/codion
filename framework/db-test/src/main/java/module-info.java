module org.jminor.framework.db.test {
  requires java.sql;
  requires junit;
  requires org.jminor.common.core;
  requires org.jminor.common.db;
  requires org.jminor.framework.db.core;
  exports org.jminor.framework.domain.testing;
}