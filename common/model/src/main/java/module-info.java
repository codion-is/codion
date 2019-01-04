module org.jminor.common.model {
  requires java.prefs;
  requires transitive org.jminor.common.db;

  exports org.jminor.common.model;
  exports org.jminor.common.model.combobox;
  exports org.jminor.common.model.table;
  exports org.jminor.common.model.valuemap;
}