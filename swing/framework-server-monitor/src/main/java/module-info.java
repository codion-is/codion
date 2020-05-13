module dev.codion.swing.framework.server.monitor {
  requires org.slf4j;
  requires jfreechart;
  requires dev.codion.framework.server;
  requires dev.codion.swing.common.ui;

  exports dev.codion.swing.framework.server.monitor;
  exports dev.codion.swing.framework.server.monitor.ui;
}