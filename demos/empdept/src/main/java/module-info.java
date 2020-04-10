module org.jminor.framework.demos.empdept {
  requires org.jminor.framework.db.http;
  requires org.jminor.framework.db.local;
  requires org.jminor.framework.db.remote;
  requires org.jminor.framework.db.test;
  requires org.jminor.swing.common.tools.ui;
  requires org.jminor.swing.framework.tools;
  requires org.jminor.swing.framework.ui;
  requires org.jminor.javafx.framework;
  requires org.jminor.framework.server;
  requires org.jminor.plugin.jasperreports;
  requires org.jminor.plugin.json;

  exports org.jminor.framework.demos.empdept.domain
          to org.jminor.framework.db.local;
  exports org.jminor.framework.demos.empdept.model
          to org.jminor.swing.framework.model;
  exports org.jminor.framework.demos.empdept.ui
          to org.jminor.swing.framework.ui;
  exports org.jminor.framework.demos.empdept.javafx
          to javafx.graphics;
  exports org.jminor.framework.demos.empdept.server
          to java.rmi;
  //for loading of reports from classpath
  opens org.jminor.framework.demos.empdept.domain
          to org.jminor.plugin.jasperreports;
}