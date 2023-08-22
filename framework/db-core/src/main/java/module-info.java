/**
 * Core framework database connection classes, such as:<br>
 * <br>
 * {@link is.codion.framework.db.EntityConnection}<br>
 * {@link is.codion.framework.db.EntityConnectionProvider}<br>
 * {@link is.codion.framework.db.condition.Condition}<br>
 * {@link is.codion.framework.db.condition.ColumnCondition}<br>
 * {@link is.codion.framework.db.condition.ForeignKeyCondition}<br>
 * @uses is.codion.framework.db.EntityConnectionProvider.Builder
 */
module is.codion.framework.db.core {
  requires org.slf4j;
  requires transitive is.codion.framework.domain;

  exports is.codion.framework.db;
  exports is.codion.framework.db.condition;

  uses is.codion.framework.db.EntityConnectionProvider.Builder;
}