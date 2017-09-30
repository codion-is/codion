module org.jminor.framework.model.unittests {
  requires java.sql;
  requires junit;
  requires org.jminor.common.core;
  requires org.jminor.common.db;
  requires org.jminor.common.model;
  requires org.jminor.framework.db.core;
  requires org.jminor.framework.db.local;
  requires org.jminor.framework.model;
  exports org.jminor.framework.model.testing;
}