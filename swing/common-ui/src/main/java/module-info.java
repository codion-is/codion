module is.codion.swing.common.ui {
  requires java.rmi;
  requires transitive is.codion.swing.common.model;

  exports is.codion.swing.common.ui;
  exports is.codion.swing.common.ui.calendar;
  exports is.codion.swing.common.ui.checkbox;
  exports is.codion.swing.common.ui.combobox;
  exports is.codion.swing.common.ui.component;
  exports is.codion.swing.common.ui.control;
  exports is.codion.swing.common.ui.dialog;
  exports is.codion.swing.common.ui.icons;
  exports is.codion.swing.common.ui.laf;
  exports is.codion.swing.common.ui.layout;
  exports is.codion.swing.common.ui.slider;
  exports is.codion.swing.common.ui.spinner;
  exports is.codion.swing.common.ui.table;
  exports is.codion.swing.common.ui.textfield;

  uses is.codion.swing.common.ui.icons.Icons;

  provides is.codion.swing.common.ui.icons.Icons
          with is.codion.swing.common.ui.icons.DefaultIcons;
}