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
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;

import static is.codion.framework.model.ForeignKeyConditionModel.foreignKeyConditionModel;
import static java.util.Objects.requireNonNull;

/**
 * A default {@link ColumnConditionModel.Factory} implementation for creating condition models.
 */
public class EntityConditionModelFactory implements ColumnConditionModel.Factory<Attribute<?>> {

	private final EntityConnectionProvider connectionProvider;

	/**
	 * Instantiates a new {@link EntityConditionModelFactory}.
	 * @param connectionProvider the connection provider
	 */
	public EntityConditionModelFactory(EntityConnectionProvider connectionProvider) {
		this.connectionProvider = requireNonNull(connectionProvider);
	}

	@Override
	public boolean includes(Attribute<?> columnIdentifier) {
		AttributeDefinition<?> definition = connectionProvider.entities()
						.definition(columnIdentifier.entityType())
						.attributes().definition(columnIdentifier);
		if (definition.hidden()) {
			return false;
		}
		if (definition instanceof ForeignKeyDefinition) {
			return true;
		}
		if (definition instanceof ColumnDefinition<?>) {
			ColumnDefinition<?> columnDefinition = (ColumnDefinition<?>) definition;

			return columnDefinition.selectable();
		}

		return false;
	}

	@Override
	public ColumnConditionModel<? extends Attribute<?>, ?> createConditionModel(Attribute<?> attribute) {
		if (attribute instanceof ForeignKey) {
			ForeignKey foreignKey = (ForeignKey) attribute;
			return foreignKeyConditionModel(foreignKey,
							this::createEqualSearchModel, this::createInSearchModel);
		}

		ColumnDefinition<?> column = definition(attribute.entityType()).columns().definition((Column<?>) attribute);

		return ColumnConditionModel.builder(attribute, attribute.type().valueClass())
						.format(column.format())
						.dateTimePattern(column.dateTimePattern())
						.build();
	}

	/**
	 * @param foreignKey the foreign key
	 * @return a search model to use for the equal value
	 */
	protected EntitySearchModel createEqualSearchModel(ForeignKey foreignKey) {
		return EntitySearchModel.builder(foreignKey.referencedType(), connectionProvider)
						.singleSelection(true)
						.build();
	}

	/**
	 * @param foreignKey the foreign key
	 * @return a search model to use for the in values
	 */
	protected EntitySearchModel createInSearchModel(ForeignKey foreignKey) {
		return EntitySearchModel.builder(foreignKey.referencedType(), connectionProvider).build();
	}

	/**
	 * @return the underlying connection provider
	 */
	protected final EntityConnectionProvider connectionProvider() {
		return connectionProvider;
	}

	/**
	 * @param entityType the entity type
	 * @return the entity definition
	 */
	protected final EntityDefinition definition(EntityType entityType) {
		return connectionProvider.entities().definition(entityType);
	}
}
