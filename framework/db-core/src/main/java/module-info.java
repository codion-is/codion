/**
 * Core framework database connection classes, such as:<br>
 * <br>
 * {@link is.codion.framework.db.EntityConnection}<br>
 * {@link is.codion.framework.db.EntityConnectionProvider}<br>
 * @uses is.codion.framework.db.EntityConnectionProvider.Builder
 */
module is.codion.framework.db.core {
  requires org.slf4j;
  requires transitive is.codion.framework.domain;

  exports is.codion.framework.db;

  uses is.codion.framework.db.EntityConnectionProvider.Builder;
}