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
package is.codion.swing.framework.ui;

import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.swing.common.ui.component.table.FilterTableColumn;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Provides table columns based on an entity definition.
 */
public final class EntityTableColumns {

	private EntityTableColumns() {}

	/**
	 * Creates columns based on the given entity definition.
	 * All attributes except hidden ones are included.
	 * @param entityDefinition the entity definition
	 * @return the columns
	 */
	public static List<FilterTableColumn<Attribute<?>>> entityTableColumns(EntityDefinition entityDefinition) {
		AtomicInteger index = new AtomicInteger();
		return requireNonNull(entityDefinition).attributes().definitions().stream()
						.filter(attributeDefinition -> !attributeDefinition.hidden())
						.map(attributeDefinition -> createColumn(attributeDefinition, index.getAndIncrement()))
						.collect(Collectors.toList());
	}

	/**
	 * Creates a column for the given attribute.
	 * @param attributeDefinition the attribute definition
	 * @param modelIndex the column model index
	 * @return the column or an empty Optional in case no column should be created for the given attribute
	 */
	private static FilterTableColumn<Attribute<?>> createColumn(AttributeDefinition<?> attributeDefinition, int modelIndex) {
		FilterTableColumn.Builder<? extends Attribute<?>> columnBuilder =
						FilterTableColumn.builder(attributeDefinition.attribute(), modelIndex)
										.headerValue(attributeDefinition.caption())
										.toolTipText(attributeDefinition.description());

		return (FilterTableColumn<Attribute<?>>) columnBuilder.build();
	}
}
