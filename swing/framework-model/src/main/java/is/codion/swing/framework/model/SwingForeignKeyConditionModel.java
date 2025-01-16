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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model;

import is.codion.common.Operator;
import is.codion.framework.model.EntitySearchModel;
import is.codion.framework.model.ForeignKeyConditionModel;
import is.codion.swing.framework.model.DefaultSwingForeignKeyConditionModel.DefaultBuilder;
import is.codion.swing.framework.model.component.EntityComboBoxModel;

/**
 * A condition model using a {@link EntityComboBoxModel} for the {@link Operator#EQUAL} operand
 * and a {@link EntitySearchModel} for the {@link Operator#IN} operands.
 * @see SwingForeignKeyConditionModel#builder()
 */
public interface SwingForeignKeyConditionModel extends ForeignKeyConditionModel {

	/**
	 * @return an {@link EntityComboBoxModel} to use for the EQUAL operand
	 * @throws IllegalStateException in case no such model is available
	 */
	EntityComboBoxModel equalComboBoxModel();

	/**
	 * @return a new {@link Builder}
	 */
	static Builder builder() {
		return new DefaultBuilder();
	}

	/**
	 * A builder for a {@link SwingForeignKeyConditionModel}
	 */
	interface Builder {

		/**
		 * @param equalComboBoxModel the combo box model to use for the EQUAl operator
		 * @return this builder
		 */
		Builder equalComboBoxModel(EntityComboBoxModel equalComboBoxModel);

		/**
		 * @throws UnsupportedOperationException
		 */
		Builder equalSearchModel(EntitySearchModel equalSearchModel);

		/**
		 * @param inSearchModel the search model to use for the IN operator
		 * @return this builder
		 */
		Builder inSearchModel(EntitySearchModel inSearchModel);

		/**
		 * @return a new {@link SwingForeignKeyConditionModel} instance
		 */
		SwingForeignKeyConditionModel build();
	}
}
