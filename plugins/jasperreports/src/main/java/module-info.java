module org.jminor.plugin.jasperreports {
  requires jasperreports;
  requires org.jminor.framework.db.core;
  requires org.jminor.swing.common.ui;

  exports org.jminor.framework.plugins.jasperreports.model;
  exports org.jminor.framework.plugins.jasperreports.ui;
}