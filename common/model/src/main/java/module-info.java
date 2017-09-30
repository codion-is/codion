module org.jminor.common.model {
  requires java.sql;
  requires java.prefs;
  requires slf4j.api;
  requires jfreechart;
  requires org.jminor.common.core;
  requires org.jminor.common.db;
  exports org.jminor.common.model;
  exports org.jminor.common.model.combobox;
  exports org.jminor.common.model.table;
  exports org.jminor.common.model.tools;
  exports org.jminor.common.model.valuemap;
}