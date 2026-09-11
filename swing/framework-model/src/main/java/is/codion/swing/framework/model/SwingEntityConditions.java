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
 * Copyright (c) 2016 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model;

import is.codion.common.model.condition.ConditionModel;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.model.EntityConditions;
import is.codion.framework.model.ForeignKeyConditionModel;
import is.codion.swing.framework.model.component.SwingEntityComboBoxModel;

import static java.util.Objects.requireNonNull;

/**
 * A Swing {@link ConditionModel} supplier using {@link SwingEntityComboBoxModel} for the EQUAL and IN operands
 * of foreign keys based on small datasets
 */
public class SwingEntityConditions extends EntityConditions {

	/**
	 * Instantiates a new {@link SwingEntityConditions}.
	 * @param entityType the entity type
	 * @param connection the connection
	 */
	public SwingEntityConditions(EntityType entityType, EntityConnection connection) {
		super(entityType, connection);
	}

	@Override
	protected ForeignKeyConditionModel condition(ForeignKey foreignKey) {
		if (definition(requireNonNull(foreignKey).referencedType()).smallDataset()) {
			// A combo box for the EQUAL operand, so the model defaults to EQUAL (the intuitive single pick),
			// and one for the IN operand, which works whether or not the referenced entity is searchable
			return ForeignKeyConditionModel.builder(foreignKey)
							.equalComboBoxModel(createEqualComboBoxModel(foreignKey))
							.inComboBoxModel(createInComboBoxModel(foreignKey))
							.caption(definition().foreignKeys().definition(foreignKey).caption())
							.build();
		}

		return super.condition(foreignKey);
	}

	/**
	 * @param foreignKey the foreign key
	 * @return a combo box model to use for the equal value
	 */
	protected SwingEntityComboBoxModel createEqualComboBoxModel(ForeignKey foreignKey) {
		return SwingEntityComboBoxModel.builder()
						.entityType(requireNonNull(foreignKey).referencedType())
						.connection(connection())
						.includeNull(true)
						.build();
	}

	/**
	 * Note that this must be a separate instance from the one returned by {@link #createEqualComboBoxModel(ForeignKey)},
	 * since the EQUAL and IN operand components exist side by side.
	 * @param foreignKey the foreign key
	 * @return a combo box model to use for the in value
	 */
	protected SwingEntityComboBoxModel createInComboBoxModel(ForeignKey foreignKey) {
		return SwingEntityComboBoxModel.builder()
						.entityType(requireNonNull(foreignKey).referencedType())
						.connection(connection())
						.includeNull(true)
						.build();
	}
}
