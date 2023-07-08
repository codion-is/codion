/**
 * Common Swing model classes.
 */
module is.codion.swing.common.model {
  requires transitive java.desktop;
  requires transitive is.codion.common.model;
  requires transitive is.codion.common.i18n;

  exports is.codion.swing.common.model.component.button;
  exports is.codion.swing.common.model.component.combobox;
  exports is.codion.swing.common.model.component.table;
  exports is.codion.swing.common.model.component.text;
  exports is.codion.swing.common.model.worker;
}