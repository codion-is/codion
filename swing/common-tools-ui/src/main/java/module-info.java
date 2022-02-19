module is.codion.swing.common.tools.ui {
  requires org.jfree.jfreechart;
  requires com.formdev.flatlaf.intellijthemes;
  requires transitive is.codion.swing.common.tools;
  requires transitive is.codion.swing.common.ui;

  exports is.codion.swing.common.tools.ui.loadtest;
  exports is.codion.swing.common.tools.ui.randomizer;
}