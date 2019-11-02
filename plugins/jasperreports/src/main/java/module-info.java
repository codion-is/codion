module org.jminor.plugin.jasperreports {
  requires jasperreports;
  requires org.jminor.framework.db.core;
  requires org.jminor.swing.common.ui;

  exports org.jminor.plugin.jasperreports.model;
  exports org.jminor.plugin.jasperreports.ui;
}