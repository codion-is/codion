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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model;

import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.swing.common.model.component.table.FilteredTableColumn;
import is.codion.swing.common.model.component.table.FilteredTableModel.ColumnFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Provides table columns based on an entity definition.
 */
public class SwingEntityColumnFactory implements ColumnFactory<Attribute<?>> {

	private final EntityDefinition entityDefinition;

	/**
	 * Instantiates a new SwingEntityColumnFactory
	 * @param entityDefinition the entity definition
	 */
	public SwingEntityColumnFactory(EntityDefinition entityDefinition) {
		this.entityDefinition = requireNonNull(entityDefinition);
	}

	@Override
	public final List<FilteredTableColumn<Attribute<?>>> createColumns() {
		AtomicInteger index = new AtomicInteger();
		return entityDefinition.attributes().definitions().stream()
						.filter(attributeDefinition -> !attributeDefinition.hidden())
						.map(attributeDefinition -> createColumn(attributeDefinition, index.getAndIncrement()))
						.filter(Optional::isPresent)
						.map(Optional::get)
						.collect(Collectors.toList());
	}

	/**
	 * Creates a column for the given attribute.
	 * @param attributeDefinition the attribute definition
	 * @param modelIndex the column model index
	 * @return the column or an empty Optional in case no column should be created for the given attribute
	 */
	protected Optional<FilteredTableColumn<Attribute<?>>> createColumn(AttributeDefinition<?> attributeDefinition, int modelIndex) {
		FilteredTableColumn.Builder<? extends Attribute<?>> columnBuilder =
						FilteredTableColumn.builder(attributeDefinition.attribute(), modelIndex)
										.headerValue(attributeDefinition.caption())
										.columnClass(attributeDefinition.attribute().type().valueClass())
										.toolTipText(attributeDefinition.description())
										.comparator(attributeComparator(attributeDefinition.attribute()));

		return Optional.of((FilteredTableColumn<Attribute<?>>) columnBuilder.build());
	}

	/**
	 * Returns a comparator for the given attribute.
	 * @param attribute the attribute
	 * @return the comparator
	 */
	protected final Comparator<?> attributeComparator(Attribute<?> attribute) {
		if (attribute instanceof ForeignKey) {
			return entityDefinition.foreignKeys().referencedBy((ForeignKey) attribute).comparator();
		}

		return entityDefinition.attributes().definition(attribute).comparator();
	}
}
