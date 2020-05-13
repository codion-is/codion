module dev.codion.swing.common.ui {
  requires java.rmi;
  requires transitive dev.codion.swing.common.model;

  exports dev.codion.swing.common.ui;
  exports dev.codion.swing.common.ui.checkbox;
  exports dev.codion.swing.common.ui.combobox;
  exports dev.codion.swing.common.ui.control;
  exports dev.codion.swing.common.ui.dialog;
  exports dev.codion.swing.common.ui.icons;
  exports dev.codion.swing.common.ui.images;
  exports dev.codion.swing.common.ui.layout;
  exports dev.codion.swing.common.ui.table;
  exports dev.codion.swing.common.ui.textfield;
  exports dev.codion.swing.common.ui.time;
  exports dev.codion.swing.common.ui.value;
  exports dev.codion.swing.common.ui.worker;

  uses dev.codion.swing.common.ui.icons.Icons;

  provides dev.codion.swing.common.ui.icons.Icons
          with dev.codion.swing.common.ui.icons.DefaultIcons;
}