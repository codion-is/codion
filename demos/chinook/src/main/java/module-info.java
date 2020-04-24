module org.jminor.framework.demos.chinook {
  requires org.jminor.common.rmi;
  requires org.jminor.framework.db.local;
  requires org.jminor.framework.db.http;
  requires org.jminor.framework.db.rmi;
  requires org.jminor.swing.common.tools.ui;
  requires org.jminor.swing.framework.tools;
  requires org.jminor.swing.framework.ui;
  requires org.jminor.javafx.framework;
  requires org.jminor.plugin.jasperreports;
  requires org.jminor.plugin.imagepanel;
  requires org.jminor.swing.plugin.ikonli.foundation;
  requires jasperreports;

  exports org.jminor.framework.demos.chinook.ui
          to org.jminor.swing.framework.ui;
  exports org.jminor.framework.demos.chinook.domain.impl
          to org.jminor.framework.db.local;
  exports org.jminor.framework.demos.chinook.tutorial
          to org.jminor.framework.db.local;
  //for loading of reports from classpath
  opens org.jminor.framework.demos.chinook.domain
          to org.jminor.plugin.jasperreports;
}