module org.jminor.swing.framework.server.monitor {
  requires java.desktop;
  requires java.rmi;
  requires slf4j.api;
  requires jfreechart;
  requires org.jminor.common.core;
  requires org.jminor.common.db;
  requires org.jminor.common.model;
  requires org.jminor.common.server;
  requires org.jminor.framework.server;
  requires org.jminor.swing.common.model;
  requires org.jminor.swing.common.ui;

  exports org.jminor.swing.framework.server.monitor;
  exports org.jminor.swing.framework.server.monitor.ui;
}