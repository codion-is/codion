module org.jminor.swing.common.model {
  requires transitive java.desktop;
  requires transitive org.jminor.common.model;

  exports org.jminor.swing.common.model;
  exports org.jminor.swing.common.model.checkbox;
  exports org.jminor.swing.common.model.combobox;
  exports org.jminor.swing.common.model.table;
}