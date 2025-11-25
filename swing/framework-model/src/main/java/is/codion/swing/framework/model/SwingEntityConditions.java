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
 * Copyright (c) 2016 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model;

import is.codion.common.model.condition.ConditionModel;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.model.EntityConditions;
import is.codion.framework.model.ForeignKeyConditionModel;
import is.codion.swing.framework.model.component.EntityComboBoxModel;

import static java.util.Objects.requireNonNull;

/**
 * A Swing {@link ConditionModel} supplier using {@link EntityComboBoxModel} for foreign keys based on small datasets
 */
public class SwingEntityConditions extends EntityConditions {

	/**
	 * Instantiates a new {@link SwingEntityConditions}.
	 * @param entityType the entity type
	 * @param connectionProvider the connection provider
	 */
	public SwingEntityConditions(EntityType entityType, EntityConnectionProvider connectionProvider) {
		super(entityType, connectionProvider);
	}

	@Override
	protected ForeignKeyConditionModel conditionModel(ForeignKey foreignKey) {
		if (definition(requireNonNull(foreignKey).referencedType()).smallDataset()) {
			return SwingForeignKeyConditionModel.builder()
							.equalComboBoxModel(createEqualComboBoxModel(foreignKey))
							.inSearchModel(createInSearchModel(foreignKey))
							.build();
		}

		return super.conditionModel(foreignKey);
	}

	/**
	 * @param foreignKey the foreign key
	 * @return a combo box model to use for the equal value
	 */
	protected EntityComboBoxModel createEqualComboBoxModel(ForeignKey foreignKey) {
		return EntityComboBoxModel.builder()
						.entityType(requireNonNull(foreignKey).referencedType())
						.connectionProvider(connectionProvider())
						.includeNull(true)
						.build();
	}
}
