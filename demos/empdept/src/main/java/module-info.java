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
  requires org.jminor.framework.plugins.jasperreports;
  requires org.jminor.framework.plugins.json;

  exports org.jminor.framework.demos.empdept.domain
          to org.jminor.framework.db.local;
  exports org.jminor.framework.demos.empdept.beans
          to org.jminor.swing.framework.model;
  exports org.jminor.framework.demos.empdept.beans.ui
          to org.jminor.swing.framework.ui;
  exports org.jminor.framework.demos.empdept.javafx
          to javafx.graphics;
}