/**
 * Common Swing UI tools.<br>
 * <br>
 * {@link is.codion.swing.common.ui.tools.loadtest.LoadTestPanel}<br>
 */
module is.codion.swing.common.ui.tools {
  requires org.jfree.jfreechart;
  requires com.formdev.flatlaf.intellijthemes;
  requires transitive is.codion.swing.common.model.tools;
  requires transitive is.codion.swing.common.ui;

  exports is.codion.swing.common.ui.tools.loadtest;
  exports is.codion.swing.common.ui.tools.randomizer;
}