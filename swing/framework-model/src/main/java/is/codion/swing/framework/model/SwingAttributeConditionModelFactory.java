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
import is.codion.common.model.condition.TableConditionModel.ConditionModelFactory;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.model.AttributeConditionModelFactory;
import is.codion.swing.framework.model.component.EntityComboBoxModel;

import java.util.Optional;

/**
 * A Swing {@link ConditionModelFactory} implementation using {@link EntityComboBoxModel} for foreign keys based on small datasets
 */
public class SwingAttributeConditionModelFactory extends AttributeConditionModelFactory {

	/**
	 * Instantiates a new {@link SwingAttributeConditionModelFactory}.
	 * @param connectionProvider the connection provider
	 */
	public SwingAttributeConditionModelFactory(EntityConnectionProvider connectionProvider) {
		super(connectionProvider);
	}

	@Override
	public Optional<ConditionModel<?>> create(Attribute<?> attribute) {
		if (attribute instanceof ForeignKey) {
			ForeignKey foreignKey = (ForeignKey) attribute;
			if (definition(foreignKey.referencedType()).smallDataset()) {
				return Optional.of(SwingForeignKeyConditionModel.builder()
								.includeEqualOperators(createEqualComboBoxModel(foreignKey))
								.includeInOperators(createInSearchModel(foreignKey))
								.build());
			}
		}

		return super.create(attribute);
	}

	/**
	 * @param foreignKey the foreign key
	 * @return a combo box model to use for the equal value
	 */
	protected EntityComboBoxModel createEqualComboBoxModel(ForeignKey foreignKey) {
		return EntityComboBoxModel.builder(foreignKey.referencedType(), connectionProvider())
						.includeNull(true)
						.build();
	}
}
