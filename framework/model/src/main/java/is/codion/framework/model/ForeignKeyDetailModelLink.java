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
 * Copyright (c) 2022 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.Configuration;
import is.codion.common.property.PropertyValue;
import is.codion.framework.domain.entity.attribute.ForeignKey;

/**
 * Represents a link between a master and detail model based on a foreign key.
 * @param <M> the {@link EntityModel} type
 * @param <E> the {@link EntityEditModel} type
 * @param <T> the {@link EntityTableModel} type
 */
public interface ForeignKeyDetailModelLink<M extends EntityModel<M, E, T>, E extends EntityEditModel, T extends EntityTableModel<E>> extends DetailModelLink<M, E, T> {

	/**
	 * Specifies whether a detail model should automatically set the foreign key value to the entity inserted by the master model.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	PropertyValue<Boolean> SET_VALUE_ON_INSERT =
					Configuration.booleanValue(ForeignKeyDetailModelLink.class.getName() + ".setValueOnInsert", true);

	/**
	 * Specifies whether a detail model should automatically search by the entity inserted by the master model.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: false
	 * </ul>
	 */
	PropertyValue<Boolean> SET_CONDITION_ON_INSERT =
					Configuration.booleanValue(ForeignKeyDetailModelLink.class.getName() + ".setConditionOnInsert", false);

	/**
	 * Specifies whether a detail model should be automatically refreshed when the selection in the master model changes.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	PropertyValue<Boolean> REFRESH_ON_SELECTION =
					Configuration.booleanValue(ForeignKeyDetailModelLink.class.getName() + ".refreshOnSelection", true);

	/**
	 * Specifies whether a detail model sets the master foreign key value to null when null or no value is selected in a master model<br>
	 * <ul>
	 * <li>Value type: Boolean<br>
	 * <li>Default value: false
	 * </ul>
	 */
	PropertyValue<Boolean> CLEAR_VALUE_ON_EMPTY_SELECTION =
					Configuration.booleanValue(ForeignKeyDetailModelLink.class.getName() + ".clearValueOnEmptySelection", false);

	/**
	 * Specifies whether a detail model clears the foreign key search condition when null or no value is selected in a master model<br>
	 * <ul>
	 * <li>Value type: Boolean<br>
	 * <li>Default value: true
	 * </ul>
	 */
	PropertyValue<Boolean> CLEAR_CONDITION_ON_EMPTY_SELECTION =
					Configuration.booleanValue(ForeignKeyDetailModelLink.class.getName() + ".clearConditionOnEmptySelection", true);

	/**
	 * @return the foreign key representing this detail model
	 */
	ForeignKey foreignKey();

	/**
	 * <p>Returns a new {@link Builder} instance.
	 * <p>Note that if the detail model contains a table model it is configured so that a query condition is required for it to show
	 * any data, via {@link EntityQueryModel#conditionRequired()}
	 * @param detailModel the detail model
	 * @param foreignKey the foreign key
	 * @param <M> the {@link EntityModel} type
	 * @param <E> the {@link EntityEditModel} type
	 * @param <T> the {@link EntityTableModel} type
	 * @return a {@link Builder} instance
	 */
	static <M extends EntityModel<M, E, T>, E extends EntityEditModel, T extends EntityTableModel<E>> Builder<M, E, T> builder(M detailModel, ForeignKey foreignKey) {
		return new DefaultForeignKeyDetailModelLink.DefaultBuilder<>(detailModel, foreignKey);
	}

	/**
	 * Builds a {@link ForeignKeyDetailModelLink}
	 * @param <M> the {@link EntityModel} type
	 * @param <E> the {@link EntityEditModel} type
	 * @param <T> the {@link EntityTableModel} type
	 */
	interface Builder<M extends EntityModel<M, E, T>, E extends EntityEditModel, T extends EntityTableModel<E>> {

		/**
		 * @param setConditionOnInsert specifies whether the detail table model should automatically search by the inserted entity
		 * when an insert is performed in a master model
		 * @return this builder
		 * @see ForeignKeyDetailModelLink#SET_CONDITION_ON_INSERT
		 */
		Builder<M, E, T> setConditionOnInsert(boolean setConditionOnInsert);

		/**
		 * @param setValueOnInsert specifies whether the detail edit model should automatically set the foreign key value to the inserted entity
		 * @return this builder
		 * @see ForeignKeyDetailModelLink#SET_VALUE_ON_INSERT
		 */
		Builder<M, E, T> setValueOnInsert(boolean setValueOnInsert);

		/**
		 * @param refreshOnSelection specifies whether the detail table model should be automatically refreshed
		 * when the foreign key condition is set according to the master model selection
		 * @return this builder
		 * @see ForeignKeyDetailModelLink#REFRESH_ON_SELECTION
		 */
		Builder<M, E, T> refreshOnSelection(boolean refreshOnSelection);

		/**
		 * @param clearValueOnEmptySelection specifies whether the detail model should set the foreign key to null when null or no value is selected in the master model.
		 * @return this builder
		 * @see ForeignKeyDetailModelLink#CLEAR_VALUE_ON_EMPTY_SELECTION
		 */
		Builder<M, E, T> clearValueOnEmptySelection(boolean clearValueOnEmptySelection);

		/**
		 * @param clearConditionOnEmptySelection specifies whether the detail table model should clear the foreign key search condition when no value is selected in the master model
		 * @return this builder
		 * @see ForeignKeyDetailModelLink#CLEAR_CONDITION_ON_EMPTY_SELECTION
		 */
		Builder<M, E, T> clearConditionOnEmptySelection(boolean clearConditionOnEmptySelection);

		/**
		 * @param active the initial active state of this link
		 * @return this builder
		 */
		Builder<M, E, T> active(boolean active);

		/**
		 * @return a {@link ForeignKeyDetailModelLink}
		 */
		ForeignKeyDetailModelLink<M, E, T> build();
	}
}
