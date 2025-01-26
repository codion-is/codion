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

import is.codion.common.property.PropertyValue;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.util.Collection;
import java.util.function.Consumer;

import static is.codion.common.Configuration.booleanValue;

/**
 * Represents a link between two entity models based on a foreign key.
 */
public interface ForeignKeyModelLink extends ModelLink {

	/**
	 * Specifies whether a linked model should automatically set the foreign key value to the entity inserted by the parent model.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	PropertyValue<Boolean> SET_VALUE_ON_INSERT =
					booleanValue(ForeignKeyModelLink.class.getName() + ".setValueOnInsert", true);

	/**
	 * Specifies whether a linked model should automatically search by the entity inserted by the parent model.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: false
	 * </ul>
	 */
	PropertyValue<Boolean> SET_CONDITION_ON_INSERT =
					booleanValue(ForeignKeyModelLink.class.getName() + ".setConditionOnInsert", false);

	/**
	 * Specifies whether a linked model should be automatically refreshed when the selection in the parent model changes.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	PropertyValue<Boolean> REFRESH_ON_SELECTION =
					booleanValue(ForeignKeyModelLink.class.getName() + ".refreshOnSelection", true);

	/**
	 * Specifies whether a linked model sets the parent foreign key value to null when null or no value is selected in a parent model<br>
	 * <ul>
	 * <li>Value type: Boolean<br>
	 * <li>Default value: false
	 * </ul>
	 */
	PropertyValue<Boolean> CLEAR_VALUE_ON_EMPTY_SELECTION =
					booleanValue(ForeignKeyModelLink.class.getName() + ".clearValueOnEmptySelection", false);

	/**
	 * Specifies whether a linked model clears the foreign key search condition when null or no value is selected in a parent model<br>
	 * <ul>
	 * <li>Value type: Boolean<br>
	 * <li>Default value: true
	 * </ul>
	 */
	PropertyValue<Boolean> CLEAR_CONDITION_ON_EMPTY_SELECTION =
					booleanValue(ForeignKeyModelLink.class.getName() + ".clearConditionOnEmptySelection", true);

	/**
	 * @return the foreign key this model link is based on
	 */
	ForeignKey foreignKey();

	/**
	 * <p>Returns a new {@link Builder} instance.
	 * <p>Note that if the linked model contains a table model it is configured so that a query condition is required for it to show
	 * any data, via {@link EntityQueryModel#conditionRequired()}
	 * @param model the model to link
	 * @param foreignKey the foreign key
	 * @return a {@link Builder} instance
	 */
	static Builder builder(EntityModel<?, ?> model, ForeignKey foreignKey) {
		return new DefaultForeignKeyModelLink.DefaultBuilder(model, foreignKey);
	}

	/**
	 * Builds a {@link ForeignKeyModelLink}
	 */
	interface Builder extends ModelLink.Builder<Builder> {

		/**
		 * <p>Note that this overrides {@link #refreshOnSelection(boolean)},
		 * {@link #clearConditionOnEmptySelection(boolean)} and {@link #clearValueOnEmptySelection(boolean)}
		 * <br>
		 * {@inheritDoc}
		 */
		@Override
		Builder onSelection(Consumer<Collection<Entity>> onSelection);

		/**
		 * <p>Note that this overrides {@link #setValueOnInsert(boolean)}
		 * <br>
		 * {@inheritDoc}
		 */
		@Override
		Builder onInsert(Consumer<Collection<Entity>> onInsert);

		/**
		 * @param setConditionOnInsert specifies whether the linked table model should automatically search by the inserted entity
		 * when an insert is performed in a parent model
		 * @return this builder
		 * @see ForeignKeyModelLink#SET_CONDITION_ON_INSERT
		 */
		Builder setConditionOnInsert(boolean setConditionOnInsert);

		/**
		 * @param setValueOnInsert specifies whether the linked edit model should automatically set the foreign key value to the inserted entity
		 * @return this builder
		 * @see ForeignKeyModelLink#SET_VALUE_ON_INSERT
		 */
		Builder setValueOnInsert(boolean setValueOnInsert);

		/**
		 * Note that only active model links respond to parent model selection by default.
		 * @param refreshOnSelection specifies whether the linked table model should be automatically refreshed
		 * when the foreign key condition is set according to the parent model selection
		 * @return this builder
		 * @see ForeignKeyModelLink#REFRESH_ON_SELECTION
		 * @see #active()
		 */
		Builder refreshOnSelection(boolean refreshOnSelection);

		/**
		 * @param clearValueOnEmptySelection specifies whether the linked model should set the foreign key to null when null or no value is selected in the parent model.
		 * @return this builder
		 * @see ForeignKeyModelLink#CLEAR_VALUE_ON_EMPTY_SELECTION
		 */
		Builder clearValueOnEmptySelection(boolean clearValueOnEmptySelection);

		/**
		 * @param clearConditionOnEmptySelection specifies whether the linked table model should clear the foreign key search condition when no value is selected in the parent model
		 * @return this builder
		 * @see ForeignKeyModelLink#CLEAR_CONDITION_ON_EMPTY_SELECTION
		 */
		Builder clearConditionOnEmptySelection(boolean clearConditionOnEmptySelection);

		/**
		 * @return a {@link ForeignKeyModelLink}
		 */
		ForeignKeyModelLink build();
	}
}
