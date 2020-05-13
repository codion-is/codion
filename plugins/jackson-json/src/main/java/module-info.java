module dev.codion.plugin.jackson.json {
  requires com.fasterxml.jackson.databind;
  requires dev.codion.framework.domain;
  requires dev.codion.framework.db.core;

  exports dev.codion.plugin.jackson.json.domain;
  exports dev.codion.plugin.jackson.json.db;
}