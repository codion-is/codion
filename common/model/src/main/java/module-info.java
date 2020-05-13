/**
 * Non-ui applicaton model classes.
 */
module dev.codion.common.model {
  requires java.prefs;
  requires transitive dev.codion.common.db;

  exports dev.codion.common.model;
  exports dev.codion.common.model.combobox;
  exports dev.codion.common.model.table;
}