module org.jminor.swing.common.ui {
  requires java.desktop;
  requires java.sql;
  requires java.rmi;
  requires org.jminor.common.core;
  requires org.jminor.common.db;
  requires org.jminor.common.model;
  requires org.jminor.framework.db.core;
  requires org.jminor.swing.common.model;
  exports org.jminor.swing.common.ui;
  exports org.jminor.swing.common.ui.checkbox;
  exports org.jminor.swing.common.ui.combobox;
  exports org.jminor.swing.common.ui.control;
  exports org.jminor.swing.common.ui.images;
  exports org.jminor.swing.common.ui.input;
  exports org.jminor.swing.common.ui.layout;
  exports org.jminor.swing.common.ui.reports;
  exports org.jminor.swing.common.ui.table;
  exports org.jminor.swing.common.ui.textfield;
  exports org.jminor.swing.common.ui.valuemap;
  exports org.jminor.swing.common.ui.worker;
}