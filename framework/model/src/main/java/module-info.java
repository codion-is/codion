/**
 * Non-ui specific application model classes.
 */
module is.codion.framework.model {
  requires org.slf4j;
  requires transitive is.codion.common.model;
  requires transitive is.codion.framework.db.core;

  exports is.codion.framework.model;
}