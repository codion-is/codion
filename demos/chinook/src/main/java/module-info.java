module dev.codion.framework.demos.chinook {
  requires dev.codion.common.rmi;
  requires dev.codion.framework.db.local;
  requires dev.codion.framework.db.http;
  requires dev.codion.framework.db.rmi;
  requires dev.codion.swing.common.tools.ui;
  requires dev.codion.swing.framework.tools;
  requires dev.codion.swing.framework.ui;
  requires dev.codion.javafx.framework;
  requires dev.codion.plugin.jasperreports;
  requires dev.codion.plugin.imagepanel;
  requires dev.codion.swing.plugin.ikonli.foundation;
  requires jasperreports;

  exports dev.codion.framework.demos.chinook.ui
          to dev.codion.swing.framework.ui;
  exports dev.codion.framework.demos.chinook.domain.impl
          to dev.codion.framework.db.local;
  exports dev.codion.framework.demos.chinook.tutorial
          to dev.codion.framework.db.local;
  //for loading of reports from classpath
  opens dev.codion.framework.demos.chinook.domain
          to dev.codion.plugin.jasperreports;
}