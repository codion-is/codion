/**
 * Non-ui specific application model classes.
 */
module dev.codion.framework.model {
  requires org.slf4j;
  requires transitive dev.codion.common.model;
  requires transitive dev.codion.framework.db.core;

  exports dev.codion.framework.model;
}