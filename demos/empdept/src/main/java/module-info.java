module is.codion.framework.demos.empdept {
  requires jasperreports;
  requires is.codion.framework.db.http;
  requires is.codion.framework.db.local;
  requires is.codion.framework.db.rmi;
  requires is.codion.framework.domain.test;
  requires is.codion.swing.common.tools.ui;
  requires is.codion.swing.framework.tools;
  requires is.codion.swing.framework.ui;
  requires is.codion.javafx.framework;
  requires is.codion.framework.server;
  requires is.codion.plugin.jasperreports;
  requires is.codion.plugin.jackson.json;

  exports is.codion.framework.demos.empdept.domain
          to is.codion.framework.domain, is.codion.framework.db.local;
  exports is.codion.framework.demos.empdept.model
          to is.codion.swing.framework.model, is.codion.swing.framework.ui;
  exports is.codion.framework.demos.empdept.ui
          to is.codion.swing.framework.ui;
  exports is.codion.framework.demos.empdept.javafx
          to javafx.graphics;
  exports is.codion.framework.demos.empdept.server
          to java.rmi;
  //for loading of reports from classpath
  opens is.codion.framework.demos.empdept.domain
          to is.codion.plugin.jasperreports;

  provides is.codion.framework.domain.Domain
          with is.codion.framework.demos.empdept.domain.EmpDept;
  provides is.codion.common.rmi.server.LoginProxy
          with is.codion.framework.demos.empdept.server.EmpDeptLoginProxy;
}