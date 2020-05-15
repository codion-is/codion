module is.codion.framework.demos.empdept {
  requires jasperreports;
  requires is.codion.framework.db.http;
  requires is.codion.framework.db.local;
  requires is.codion.framework.db.rmi;
  requires is.codion.framework.db.test;
  requires is.codion.swing.common.tools.ui;
  requires is.codion.swing.framework.tools;
  requires is.codion.swing.framework.ui;
  requires is.codion.javafx.framework;
  requires is.codion.framework.server;
  requires is.codion.plugin.jasperreports;
  requires is.codion.plugin.json;

  exports is.codion.framework.demos.empdept.domain
          to is.codion.framework.domain, is.codion.framework.db.local;
  exports is.codion.framework.demos.empdept.model
          to is.codion.swing.framework.model;
  exports is.codion.framework.demos.empdept.ui
          to is.codion.swing.framework.ui;
  exports is.codion.framework.demos.empdept.javafx
          to javafx.graphics;
  exports is.codion.framework.demos.empdept.server
          to java.rmi;
  //for loading of reports from classpath
  opens is.codion.framework.demos.empdept.domain
          to is.codion.plugin.jasperreports;
}