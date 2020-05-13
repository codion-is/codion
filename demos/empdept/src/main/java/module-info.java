module dev.codion.framework.demos.empdept {
  requires jasperreports;
  requires dev.codion.framework.db.http;
  requires dev.codion.framework.db.local;
  requires dev.codion.framework.db.rmi;
  requires dev.codion.framework.db.test;
  requires dev.codion.swing.common.tools.ui;
  requires dev.codion.swing.framework.tools;
  requires dev.codion.swing.framework.ui;
  requires dev.codion.javafx.framework;
  requires dev.codion.framework.server;
  requires dev.codion.plugin.jasperreports;
  requires dev.codion.plugin.json;

  exports dev.codion.framework.demos.empdept.domain
          to dev.codion.framework.domain, dev.codion.framework.db.local;
  exports dev.codion.framework.demos.empdept.model
          to dev.codion.swing.framework.model;
  exports dev.codion.framework.demos.empdept.ui
          to dev.codion.swing.framework.ui;
  exports dev.codion.framework.demos.empdept.javafx
          to javafx.graphics;
  exports dev.codion.framework.demos.empdept.server
          to java.rmi;
  //for loading of reports from classpath
  opens dev.codion.framework.demos.empdept.domain
          to dev.codion.plugin.jasperreports;
}