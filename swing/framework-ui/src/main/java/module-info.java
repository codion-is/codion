module org.jminor.swing.framework.ui {
  requires java.sql;
  requires java.desktop;
  requires slf4j.api;
  requires org.jminor.common.core;
  requires org.jminor.common.db;
  requires org.jminor.common.model;
  requires org.jminor.common.server;
  requires org.jminor.framework.db.core;
  requires org.jminor.framework.model;
  requires org.jminor.swing.framework.model;
  requires org.jminor.swing.common.ui;
  requires org.jminor.swing.common.model;
  exports org.jminor.swing.framework.ui;
  exports org.jminor.swing.framework.ui.reporting;
}