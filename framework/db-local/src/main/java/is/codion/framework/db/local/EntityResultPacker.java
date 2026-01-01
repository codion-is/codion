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
 * Copyright (c) 2019 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.local;

import is.codion.common.db.result.ResultPacker;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.TransientAttributeDefinition;

import org.jspecify.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

/**
 * Handles packing Entity query results.
 */
final class EntityResultPacker implements ResultPacker<Entity> {

	private static final Function<EntityDefinition, List<AttributeDefinition<?>>> INIT_TRANSIENT_ATTRIBUTES =
					EntityResultPacker::initializeTransientAttributes;
	private static final Function<EntityDefinition, List<ColumnDefinition<?>>> INIT_NON_SELECTED_COLUMNS =
					EntityResultPacker::initializeNonSelectedColumns;
	private static final Map<EntityDefinition, List<AttributeDefinition<?>>> TRANSIENT_ATTRIBUTES = new ConcurrentHashMap<>();
	private static final Map<EntityDefinition, List<ColumnDefinition<?>>> NON_SELECTED_COLUMNS = new ConcurrentHashMap<>();

	private final EntityDefinition entityDefinition;
	private final List<ColumnDefinition<?>> columnDefinitions;
	private final List<AttributeDefinition<?>> transientAttributes;
	private final List<ColumnDefinition<?>> nonSelectedColumns;
	private final boolean customSelectColumns;

	/**
	 * @param entityDefinition the entity definition
	 * @param columnDefinitions the column definitions
	 */
	EntityResultPacker(EntityDefinition entityDefinition, List<ColumnDefinition<?>> columnDefinitions) {
		this.entityDefinition = entityDefinition;
		this.columnDefinitions = columnDefinitions;
		this.transientAttributes = TRANSIENT_ATTRIBUTES.computeIfAbsent(entityDefinition, INIT_TRANSIENT_ATTRIBUTES);
		this.nonSelectedColumns = NON_SELECTED_COLUMNS.computeIfAbsent(entityDefinition, INIT_NON_SELECTED_COLUMNS);
		this.customSelectColumns = entityDefinition.selectQuery()
						.map(query -> query.columns() != null)
						.orElse(false);
	}

	@Override
	public Entity get(ResultSet resultSet) throws SQLException {
		int attributeCount = columnDefinitions.size() + transientAttributes.size() + nonSelectedColumns.size();
		Map<Attribute<?>, Object> values = new HashMap<>((int) (attributeCount / 0.75f) + 1);
		addResultSetValues(resultSet, values);
		addTransientNullValues(values);
		addNonSelectedNullValues(values);

		return entityDefinition.entity(values);
	}

	private void addResultSetValues(ResultSet resultSet, Map<Attribute<?>, @Nullable Object> values) throws SQLException {
		for (int i = 0; i < columnDefinitions.size(); i++) {
			ColumnDefinition<Object> columnDefinition = (ColumnDefinition<Object>) columnDefinitions.get(i);
			try {
				values.put(columnDefinition.attribute(), customSelectColumns ?
								columnDefinition.get(resultSet) :
								columnDefinition.get(resultSet, i + 1));
			}
			catch (Exception e) {
				throw new SQLException("Exception fetching: " + columnDefinition + ", entity: " +
								entityDefinition.type() + " [" + e.getMessage() + "]", e);
			}
		}
	}

	private void addTransientNullValues(Map<Attribute<?>, @Nullable Object> values) {
		for (int i = 0; i < transientAttributes.size(); i++) {
			values.put(transientAttributes.get(i).attribute(), null);
		}
	}

	private void addNonSelectedNullValues(Map<Attribute<?>, @Nullable Object> values) {
		for (int i = 0; i < nonSelectedColumns.size(); i++) {
			values.putIfAbsent(nonSelectedColumns.get(i).attribute(), null);
		}
	}

	private static List<AttributeDefinition<?>> initializeTransientAttributes(EntityDefinition entityDefinition) {
		return entityDefinition.attributes().definitions().stream()
						.filter(TransientAttributeDefinition.class::isInstance)
						.collect(toList());
	}

	private static List<ColumnDefinition<?>> initializeNonSelectedColumns(EntityDefinition entityDefinition) {
		return entityDefinition.columns().definitions().stream()
						.filter(columnDefinition -> !columnDefinition.selected())
						.collect(toList());
	}
}
