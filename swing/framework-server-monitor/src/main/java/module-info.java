/**
 * Server monitor.
 */
module is.codion.swing.framework.server.monitor {
  requires org.slf4j;
  requires org.jfree.jfreechart;
  requires com.formdev.flatlaf.intellijthemes;
  requires is.codion.framework.server;
  requires is.codion.swing.common.ui;

  exports is.codion.swing.framework.server.monitor;
  exports is.codion.swing.framework.server.monitor.ui;
}