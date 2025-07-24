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
 * Copyright (c) 2019 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.local;

import is.codion.common.db.result.ResultPacker;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.TransientAttributeDefinition;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Handles packing Entity query results.
 */
final class EntityResultPacker implements ResultPacker<Entity> {

	private static final Function<EntityDefinition, List<TransientAttributeDefinition<?>>> INIT_NON_DERIVED_TRANSIENT_ATTRIBUTES =
					EntityResultPacker::initializeNonDerivedTransientAttributes;
	private static final Function<EntityDefinition, List<ColumnDefinition<?>>> INIT_NON_SELECTED_COLUMNS =
					EntityResultPacker::initializeNonSelectedColumns;
	private static final Map<EntityDefinition, List<TransientAttributeDefinition<?>>> NON_DERIVED_TRANSIENT_ATTRIBUTES = new ConcurrentHashMap<>();
	private static final Map<EntityDefinition, List<ColumnDefinition<?>>> NON_SELECTED_COLUMNS = new ConcurrentHashMap<>();

	private final EntityDefinition entityDefinition;
	private final List<ColumnDefinition<?>> columnDefinitions;
	private final List<TransientAttributeDefinition<?>> nonDerivedTransientAttributes;
	private final List<ColumnDefinition<?>> nonSelectedColumns;

	/**
	 * @param entityDefinition the entity definition
	 * @param columnDefinitions the column definitions in the same order as they appear in the ResultSet
	 */
	EntityResultPacker(EntityDefinition entityDefinition, List<ColumnDefinition<?>> columnDefinitions) {
		this.entityDefinition = entityDefinition;
		this.columnDefinitions = columnDefinitions;
		this.nonDerivedTransientAttributes = NON_DERIVED_TRANSIENT_ATTRIBUTES.computeIfAbsent(entityDefinition, INIT_NON_DERIVED_TRANSIENT_ATTRIBUTES);
		this.nonSelectedColumns = NON_SELECTED_COLUMNS.computeIfAbsent(entityDefinition, INIT_NON_SELECTED_COLUMNS);
	}

	@Override
	public Entity get(ResultSet resultSet) throws SQLException {
		int attributeCount = columnDefinitions.size() + nonDerivedTransientAttributes.size() + nonSelectedColumns.size();
		Map<Attribute<?>, Object> values = new HashMap<>((int) (attributeCount / 0.75f) + 1);
		addResultSetValues(resultSet, values);
		addTransientNullValues(values);
		addNonSelectedNullValues(values);

		return entityDefinition.entity(values);
	}

	private void addResultSetValues(ResultSet resultSet, Map<Attribute<?>, Object> values) throws SQLException {
		for (int i = 0; i < columnDefinitions.size(); i++) {
			ColumnDefinition<Object> columnDefinition = (ColumnDefinition<Object>) columnDefinitions.get(i);
			try {
				values.put(columnDefinition.attribute(), columnDefinition.get(resultSet, i + 1));
			}
			catch (Exception e) {
				throw new SQLException("Exception fetching: " + columnDefinition + ", entity: " +
								entityDefinition.type() + " [" + e.getMessage() + "]", e);
			}
		}
	}

	private void addTransientNullValues(Map<Attribute<?>, Object> values) {
		for (int i = 0; i < nonDerivedTransientAttributes.size(); i++) {
			values.put(nonDerivedTransientAttributes.get(i).attribute(), null);
		}
	}

	private void addNonSelectedNullValues(Map<Attribute<?>, Object> values) {
		for (int i = 0; i < nonSelectedColumns.size(); i++) {
			values.putIfAbsent(nonSelectedColumns.get(i).attribute(), null);
		}
	}

	private static List<TransientAttributeDefinition<?>> initializeNonDerivedTransientAttributes(EntityDefinition entityDefinition) {
		return entityDefinition.attributes().definitions().stream()
						.filter(TransientAttributeDefinition.class::isInstance)
						.map(attributeDefinition -> (TransientAttributeDefinition<?>) attributeDefinition)
						.filter(transientAttributeDefinition -> !transientAttributeDefinition.derived())
						.collect(Collectors.toList());
	}

	private static List<ColumnDefinition<?>> initializeNonSelectedColumns(EntityDefinition entityDefinition) {
		return entityDefinition.columns().definitions().stream()
						.filter(columnDefinition -> !columnDefinition.selected())
						.collect(Collectors.toList());
	}
}
