module org.jminor.framework.demos.chinook {
  requires java.sql;
  requires java.desktop;
  requires javafx.graphics;
  requires org.jminor.common.core;
  requires org.jminor.common.db;
  requires org.jminor.common.model;
  requires org.jminor.framework.db.core;
  requires org.jminor.framework.db.local;
  requires org.jminor.framework.model;
  requires org.jminor.swing.common.model;
  requires org.jminor.swing.common.ui;
  requires org.jminor.swing.framework.model;
  requires org.jminor.swing.framework.ui;
  requires org.jminor.swing.framework.tools;
  requires org.jminor.javafx.framework;
  requires org.jminor.framework.plugins.jasperreports;
  requires org.jminor.swing.common.tools.ui;
  requires org.jminor.swing.common.tools;

  uses org.jminor.framework.db.EntityConnectionProvider;
}