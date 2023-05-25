/**
 * Chinook demo.
 */
module is.codion.framework.demos.chinook {
  requires is.codion.common.rmi;
  requires is.codion.framework.db.local;
  requires is.codion.framework.db.http;
  requires is.codion.framework.db.rmi;
  requires is.codion.swing.common.tools.ui;
  requires is.codion.swing.framework.tools;
  requires is.codion.swing.framework.ui;
  requires is.codion.javafx.framework;
  requires is.codion.plugin.jasperreports;
  requires is.codion.plugin.imagepanel;
  requires jasperreports;
  requires com.formdev.flatlaf.intellijthemes;

  exports is.codion.framework.demos.chinook.model
          to is.codion.swing.framework.model, is.codion.swing.framework.ui;
  exports is.codion.framework.demos.chinook.ui
          to is.codion.swing.framework.ui;
  exports is.codion.framework.demos.chinook.tutorial
          to is.codion.framework.db.local;
  //for loading of reports from classpath, accessing default methods in EntityType interfaces and resource bundles
  opens is.codion.framework.demos.chinook.domain;

  provides is.codion.framework.domain.Domain
          with is.codion.framework.demos.chinook.domain.impl.ChinookImpl;
  provides is.codion.common.rmi.server.LoginProxy
          with is.codion.framework.demos.chinook.server.ChinookLoginProxy;
}