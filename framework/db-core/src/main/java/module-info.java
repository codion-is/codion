/**
 * Framework database core connection classes.
 * @uses is.codion.framework.db.EntityConnectionProvider.Builder
 */
module is.codion.framework.db.core {
  requires org.slf4j;
  requires transitive is.codion.framework.domain;

  exports is.codion.framework.db;
  exports is.codion.framework.db.condition;

  uses is.codion.framework.db.EntityConnectionProvider.Builder;
}