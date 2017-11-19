module org.jminor.framework.plugins.jasperreports {
  requires jasperreports;
  requires java.sql;
  requires java.desktop;
  requires org.jminor.common.core;
  requires org.jminor.common.db;
  requires org.jminor.framework.db.core;
  requires org.jminor.swing.common.ui;

  exports org.jminor.framework.plugins.jasperreports.model;
  exports org.jminor.framework.plugins.jasperreports.ui;
}