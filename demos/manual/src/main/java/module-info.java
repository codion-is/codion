module org.jminor.framework.demos.manual {
  requires java.desktop;
  requires jasperreports;
  requires org.jminor.common.core;
  requires org.jminor.common.http;
  requires org.jminor.dbms.h2database;
  requires org.jminor.framework.db.local;
  requires org.jminor.framework.db.rmi;
  requires org.jminor.framework.db.http;
  requires org.jminor.framework.server;
  requires org.jminor.framework.servlet;
  requires org.jminor.swing.common.ui;
  requires org.jminor.swing.framework.model;
  requires org.jminor.swing.framework.tools;
  requires org.jminor.swing.framework.ui;
  requires org.jminor.plugin.jasperreports;
  requires org.jminor.framework.db.test;
  requires org.junit.jupiter.api;

  exports org.jminor.framework.demos.manual.store.domain;
  exports org.jminor.framework.demos.manual.store.ui;
}