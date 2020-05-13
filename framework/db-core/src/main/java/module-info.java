/**
 * Framework database core connection classes.
 * @uses dev.codion.framework.db.EntityConnectionProvider
 */
module dev.codion.framework.db.core {
  requires org.slf4j;
  requires transitive dev.codion.framework.domain;

  exports dev.codion.framework.db;
  exports dev.codion.framework.db.condition;

  uses dev.codion.framework.db.EntityConnectionProvider;
}