module org.jminor.swing.framework.server.monitor {
  requires slf4j.api;
  requires jfreechart;
  requires org.jminor.framework.db.remote;
  requires org.jminor.framework.server;
  requires org.jminor.swing.common.ui;

  exports org.jminor.swing.framework.server.monitor;
  exports org.jminor.swing.framework.server.monitor.ui;
}