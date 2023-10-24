/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2023, Björn Darri Sigurðsson.
 */
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
 * {@link is.codion.framework.domain.entity.condition.Condition}<br>
 * {@link is.codion.framework.domain.entity.condition.ColumnCondition}<br>
 * {@link is.codion.framework.domain.entity.condition.ForeignKeyCondition}<br>
 */
module is.codion.framework.domain {
  requires org.slf4j;
  requires transitive is.codion.common.db;

  exports is.codion.framework.domain.entity;
  exports is.codion.framework.domain.entity.attribute;
  exports is.codion.framework.domain.entity.condition;
  exports is.codion.framework.domain.entity.exception;
  exports is.codion.framework.domain.entity.query;
  exports is.codion.framework.domain;

  uses is.codion.framework.domain.Domain;
}