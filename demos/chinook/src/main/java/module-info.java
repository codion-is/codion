module org.jminor.framework.demos.chinook {
  requires org.jminor.common.remote;
  requires org.jminor.framework.db.local;
  requires org.jminor.framework.db.http;
  requires org.jminor.framework.db.remote;
  requires org.jminor.swing.common.tools.ui;
  requires org.jminor.swing.framework.tools;
  requires org.jminor.swing.framework.ui;
  requires org.jminor.javafx.framework;
  requires org.jminor.plugin.jasperreports;
  requires org.jminor.plugin.imagepanel;
  requires jasperreports;

  exports org.jminor.framework.demos.chinook.ui
          to org.jminor.swing.framework.ui;
  exports org.jminor.framework.demos.chinook.domain.impl
          to org.jminor.framework.db.local;
  exports org.jminor.framework.demos.chinook.tutorial
          to org.jminor.framework.db.local;
}