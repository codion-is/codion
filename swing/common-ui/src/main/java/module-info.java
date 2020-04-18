module org.jminor.swing.common.ui {
  requires java.rmi;
  requires transitive org.jminor.swing.common.model;

  exports org.jminor.swing.common.ui;
  exports org.jminor.swing.common.ui.checkbox;
  exports org.jminor.swing.common.ui.combobox;
  exports org.jminor.swing.common.ui.control;
  exports org.jminor.swing.common.ui.dialog;
  exports org.jminor.swing.common.ui.icons;
  exports org.jminor.swing.common.ui.images;
  exports org.jminor.swing.common.ui.layout;
  exports org.jminor.swing.common.ui.table;
  exports org.jminor.swing.common.ui.textfield;
  exports org.jminor.swing.common.ui.time;
  exports org.jminor.swing.common.ui.value;
  exports org.jminor.swing.common.ui.worker;

  uses org.jminor.swing.common.ui.icons.Icons;

  provides org.jminor.swing.common.ui.icons.Icons
          with org.jminor.swing.common.ui.icons.DefaultIcons;
}