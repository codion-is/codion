/**
 * Non-ui applicaton model classes.
 */
module is.codion.common.model {
  requires java.prefs;
  requires transitive is.codion.common.core;

  exports is.codion.common.model;
  exports is.codion.common.model.table;
}