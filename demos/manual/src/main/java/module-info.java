module org.jminor.framework.demos.manual {
  requires java.desktop;
  requires org.jminor.common.core;
  requires org.jminor.framework.db.local;
  requires org.jminor.framework.db.remote;
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