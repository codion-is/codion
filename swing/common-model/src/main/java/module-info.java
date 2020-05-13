module dev.codion.swing.common.model {
  requires transitive java.desktop;
  requires transitive dev.codion.common.model;

  exports dev.codion.swing.common.model.checkbox;
  exports dev.codion.swing.common.model.combobox;
  exports dev.codion.swing.common.model.table;
  exports dev.codion.swing.common.model.textfield;
}