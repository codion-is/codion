module is.codion.swing.common.ui {
  requires java.rmi;
  requires transitive is.codion.swing.common.model;

  exports is.codion.swing.common.ui;
  exports is.codion.swing.common.ui.component;
  exports is.codion.swing.common.ui.component.calendar;
  exports is.codion.swing.common.ui.component.checkbox;
  exports is.codion.swing.common.ui.component.combobox;
  exports is.codion.swing.common.ui.component.panel;
  exports is.codion.swing.common.ui.component.slider;
  exports is.codion.swing.common.ui.component.spinner;
  exports is.codion.swing.common.ui.component.table;
  exports is.codion.swing.common.ui.component.text;
  exports is.codion.swing.common.ui.control;
  exports is.codion.swing.common.ui.dialog;
  exports is.codion.swing.common.ui.icon;
  exports is.codion.swing.common.ui.laf;
  exports is.codion.swing.common.ui.layout;
}