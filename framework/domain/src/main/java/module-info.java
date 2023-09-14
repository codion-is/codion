/**
 * Framework domain model classes, such as:<br>
 * <br>
 * {@link is.codion.framework.domain.Domain}<br>
 * {@link is.codion.framework.domain.entity.Entity}<br>
 * {@link is.codion.framework.domain.entity.EntityDefinition}<br>
 * {@link is.codion.framework.domain.entity.attribute.Attribute}<br>
 * {@link is.codion.framework.domain.entity.attribute.AttributeDefinition}<br>
 * {@link is.codion.framework.domain.entity.attribute.Column}<br>
 * {@link is.codion.framework.domain.entity.attribute.ColumnDefinition}<br>
 * {@link is.codion.framework.domain.entity.attribute.ForeignKey}<br>
 * {@link is.codion.framework.domain.entity.attribute.ForeignKeyDefinition}<br>
 * {@link is.codion.framework.domain.entity.attribute.Condition}<br>
 * {@link is.codion.framework.domain.entity.attribute.ColumnCondition}<br>
 * {@link is.codion.framework.domain.entity.attribute.ForeignKeyCondition}<br>
 */
module is.codion.framework.domain {
  requires org.slf4j;
  requires transitive is.codion.common.db;

  exports is.codion.framework.domain.entity;
  exports is.codion.framework.domain.entity.attribute;
  exports is.codion.framework.domain.entity.exception;
  exports is.codion.framework.domain.entity.query;
  exports is.codion.framework.domain;

  uses is.codion.framework.domain.Domain;
}