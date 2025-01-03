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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
/**
 * Framework domain model classes, such as:
 * <ul>
 * <li>{@link is.codion.framework.domain.Domain}
 * <li>{@link is.codion.framework.domain.entity.Entity}
 * <li>{@link is.codion.framework.domain.entity.EntityDefinition}
 * <li>{@link is.codion.framework.domain.entity.attribute.Attribute}
 * <li>{@link is.codion.framework.domain.entity.attribute.AttributeDefinition}
 * <li>{@link is.codion.framework.domain.entity.attribute.Column}
 * <li>{@link is.codion.framework.domain.entity.attribute.ColumnDefinition}
 * <li>{@link is.codion.framework.domain.entity.attribute.ForeignKey}
 * <li>{@link is.codion.framework.domain.entity.attribute.ForeignKeyDefinition}
 * <li>{@link is.codion.framework.domain.entity.condition.Condition}
 * <li>{@link is.codion.framework.domain.entity.condition.ColumnCondition}
 * <li>{@link is.codion.framework.domain.entity.condition.ForeignKeyCondition}
 * </ul>
 * @uses is.codion.framework.domain.Domain
 */
module is.codion.framework.domain {
	requires transitive is.codion.common.db;

	exports is.codion.framework.domain.entity;
	exports is.codion.framework.domain.entity.attribute;
	exports is.codion.framework.domain.entity.condition;
	exports is.codion.framework.domain.entity.exception;
	exports is.codion.framework.domain.entity.query;
	exports is.codion.framework.domain;

	uses is.codion.framework.domain.Domain;
}