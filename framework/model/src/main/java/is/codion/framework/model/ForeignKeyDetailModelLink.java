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
import is.codion.common.state.State;
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
	 * @return the {@link State} controlling whether the detail table model should automatically search by the inserted entity
	 * when an insert is performed in a master model
	 * @see ForeignKeyDetailModelLink#SET_CONDITION_ON_INSERT
	 */
	State setConditionOnInsert();

	/**
	 * @return the {@link State} controlling whether the detail edit model should automatically set the foreign key value to the inserted entity
	 * @see ForeignKeyDetailModelLink#SET_VALUE_ON_INSERT
	 */
	State setValueOnInsert();

	/**
	 * @return the {@link State} controlling whether the detail table model should be automatically refreshed
	 * when the foreign key condition is set according to the master model selection
	 * @see ForeignKeyDetailModelLink#REFRESH_ON_SELECTION
	 */
	State refreshOnSelection();

	/**
	 * Returns the {@link State} controlling whether the detail model should set the foreign key to null when null or no value is selected in the master model.
	 * @return the {@link State} controlling whether a null selection should result in the foreign key being set to null
	 * @see ForeignKeyDetailModelLink#CLEAR_VALUE_ON_EMPTY_SELECTION
	 */
	State clearValueOnEmptySelection();

	/**
	 * Returns the {@link State} controlling whether the detail table model should clear the foreign key search condition when no value is selected in the master model
	 * @return the {@link State} controlling whether an empty selection should result in the foreign key search condition being cleared
	 * @see ForeignKeyDetailModelLink#CLEAR_CONDITION_ON_EMPTY_SELECTION
	 */
	State clearConditionOnEmptySelection();
}
