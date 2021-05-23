module is.codion.swing.common.ui {
  requires java.rmi;
  requires com.github.lgooddatepicker;
  requires transitive is.codion.swing.common.model;

  exports is.codion.swing.common.ui;
  exports is.codion.swing.common.ui.checkbox;
  exports is.codion.swing.common.ui.combobox;
  exports is.codion.swing.common.ui.component;
  exports is.codion.swing.common.ui.control;
  exports is.codion.swing.common.ui.dialog;
  exports is.codion.swing.common.ui.icons;
  exports is.codion.swing.common.ui.layout;
  exports is.codion.swing.common.ui.table;
  exports is.codion.swing.common.ui.textfield;
  exports is.codion.swing.common.ui.time;
  exports is.codion.swing.common.ui.value;
  exports is.codion.swing.common.ui.worker;

  uses is.codion.swing.common.ui.icons.Icons;

  provides is.codion.swing.common.ui.icons.Icons
          with is.codion.swing.common.ui.icons.DefaultIcons;
}