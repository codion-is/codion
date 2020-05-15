module is.codion.plugin.jackson.json {
  requires com.fasterxml.jackson.databind;
  requires is.codion.framework.domain;
  requires is.codion.framework.db.core;

  exports is.codion.plugin.jackson.json.domain;
  exports is.codion.plugin.jackson.json.db;
}