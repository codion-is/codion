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

import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * Configures table columns based on an entity definition.
 */
public final class EntityTableColumns implements Consumer<FilterTableColumn.Builder<Attribute<?>>> {

	private final EntityDefinition entityDefinition;

	private EntityTableColumns(EntityDefinition entityDefinition) {
		this.entityDefinition = entityDefinition;
	}

	/**
	 * @param entityDefinition the entity definition
	 * @return a new {@link EntityTableColumns} instance
	 */
	public static EntityTableColumns entityTableColumns(EntityDefinition entityDefinition) {
		return new EntityTableColumns(requireNonNull(entityDefinition));
	}

	@Override
	public void accept(FilterTableColumn.Builder<Attribute<?>> builder) {
		AttributeDefinition<?> attributeDefinition = entityDefinition.attributes().definition(builder.identifier());

		builder.toolTipText(attributeDefinition.description().orElse(null));
	}
}
