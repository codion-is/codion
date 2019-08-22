/**
 * Framework database core connection classes.
 * @uses org.jminor.framework.db.EntityConnectionProvider
 */
module org.jminor.framework.db.core {
  requires org.slf4j;
  requires transitive org.jminor.framework.domain;

  exports org.jminor.framework.db;
  exports org.jminor.framework.db.condition;

  uses org.jminor.framework.db.EntityConnectionProvider;
}