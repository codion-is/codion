/**
 * Manual demo.
 */
module is.codion.framework.demos.manual {
  requires java.desktop;
  requires jasperreports;
  requires is.codion.common.core;
  requires is.codion.common.http;
  requires is.codion.dbms.h2database;
  requires is.codion.framework.db.local;
  requires is.codion.framework.db.rmi;
  requires is.codion.framework.db.http;
  requires is.codion.framework.server;
  requires is.codion.framework.servlet;
  requires is.codion.swing.common.ui;
  requires is.codion.swing.framework.model;
  requires is.codion.swing.framework.tools;
  requires is.codion.swing.framework.ui;
  requires is.codion.plugin.jasperreports;
  requires is.codion.framework.domain.test;
  requires org.junit.jupiter.api;
  requires com.formdev.flatlaf.intellijthemes;

  exports is.codion.framework.demos.manual.store.domain;
  exports is.codion.framework.demos.manual.store.minimal.domain;
  exports is.codion.framework.demos.manual.store.model;
  exports is.codion.framework.demos.manual.store.ui;
}