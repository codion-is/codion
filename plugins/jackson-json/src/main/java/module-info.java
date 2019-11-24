module org.jminor.plugin.jackson.json {
  requires com.fasterxml.jackson.databind;
  requires org.jminor.framework.domain;
  requires org.jminor.framework.db.core;

  exports org.jminor.plugin.jackson.json.domain;
  exports org.jminor.plugin.jackson.json.db;
}