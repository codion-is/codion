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
 * Copyright (c) 2010 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.model.condition.ConditionModel;
import is.codion.common.model.condition.TableConditionModel.ConditionModelFactory;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * A default {@link ConditionModelFactory} implementation for creating Entity based column condition models.
 */
public class AttributeConditionModelFactory implements ConditionModelFactory<Attribute<?>> {

	private final EntityConnectionProvider connectionProvider;

	/**
	 * Instantiates a new {@link AttributeConditionModelFactory}.
	 * @param connectionProvider the connection provider
	 */
	public AttributeConditionModelFactory(EntityConnectionProvider connectionProvider) {
		this.connectionProvider = requireNonNull(connectionProvider);
	}

	@Override
	public Optional<ConditionModel<?>> create(Attribute<?> attribute) {
		if (attribute instanceof ForeignKey) {
			ForeignKey foreignKey = (ForeignKey) attribute;
			return Optional.of(ForeignKeyConditionModel.builder()
							.equalSearchModel(createEqualSearchModel(foreignKey))
							.inSearchModel(createInSearchModel(foreignKey))
							.build());
		}

		ColumnDefinition<?> column = definition(attribute.entityType()).columns().definition((Column<?>) attribute);
		return Optional.of(ConditionModel.builder(attribute.type().valueClass())
						.format(column.format())
						.dateTimePattern(column.dateTimePattern())
						.build());
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
