module dev.codion.framework.demos.manual {
  requires java.desktop;
  requires jasperreports;
  requires dev.codion.common.core;
  requires dev.codion.common.http;
  requires dev.codion.dbms.h2database;
  requires dev.codion.framework.db.local;
  requires dev.codion.framework.db.rmi;
  requires dev.codion.framework.db.http;
  requires dev.codion.framework.server;
  requires dev.codion.framework.servlet;
  requires dev.codion.swing.common.ui;
  requires dev.codion.swing.framework.model;
  requires dev.codion.swing.framework.tools;
  requires dev.codion.swing.framework.ui;
  requires dev.codion.plugin.jasperreports;
  requires dev.codion.framework.db.test;
  requires org.junit.jupiter.api;

  exports dev.codion.framework.demos.manual.store.domain;
  exports dev.codion.framework.demos.manual.store.ui;
}