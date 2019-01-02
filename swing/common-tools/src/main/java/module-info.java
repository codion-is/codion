module org.jminor.swing.common.tools {
  requires java.sql;
  requires java.desktop;
  requires slf4j.api;
  requires jfreechart;
  requires org.jminor.common.core;
  requires org.jminor.common.db;
  requires org.jminor.common.model;
  exports org.jminor.swing.common.tools;
}