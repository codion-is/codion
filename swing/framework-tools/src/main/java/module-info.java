module org.jminor.swing.framework.tools {
  requires java.desktop;
  requires java.sql;
  requires slf4j.api;
  requires org.jminor.common.core;
  requires org.jminor.common.db;
  requires org.jminor.common.model;
  requires org.jminor.framework.db.core;
  requires org.jminor.framework.model;
  requires org.jminor.swing.common.tools;
  requires org.jminor.swing.common.model;

  exports org.jminor.swing.framework.tools;
}