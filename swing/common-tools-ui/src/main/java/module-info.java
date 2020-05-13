module dev.codion.swing.common.tools.ui {
  requires jfreechart;
  requires transitive dev.codion.swing.common.tools;
  requires transitive dev.codion.swing.common.ui;

  exports dev.codion.swing.common.tools.ui.loadtest;
  exports dev.codion.swing.common.tools.ui.randomizer;
}