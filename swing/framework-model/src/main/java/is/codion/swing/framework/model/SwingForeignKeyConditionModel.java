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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.model.AbstractForeignKeyConditionModel;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.framework.model.component.EntityComboBoxModel;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * A condition model using a {@link EntityComboBoxModel} for the {@link #equalValue()}
 * and a {@link EntitySearchModel} for the {@link #inValues()}.
 * @see #swingForeignKeyConditionModel(ForeignKey, Function, Function)
 */
public final class SwingForeignKeyConditionModel extends AbstractForeignKeyConditionModel {

	private final EntityComboBoxModel equalComboBoxModel;
	private final EntitySearchModel inSearchModel;

	private boolean updatingModel = false;

	private SwingForeignKeyConditionModel(ForeignKey foreignKey,
																				Function<ForeignKey, EntityComboBoxModel> equalComboBoxModel,
																				Function<ForeignKey, EntitySearchModel> inSearchModel) {
		super(foreignKey);
		this.inSearchModel = requireNonNull(inSearchModel.apply(foreignKey));
		this.equalComboBoxModel = requireNonNull(equalComboBoxModel).apply(foreignKey);
		bindComboBoxEvents();
		bindSearchModelEvents();
	}

	/**
	 * @return the combo box model controlling the equal value
	 */
	public EntityComboBoxModel equalComboBoxModel() {
		return equalComboBoxModel;
	}

	@Override
	public EntitySearchModel inSearchModel() {
		return inSearchModel;
	}

	/**
	 * @param foreignKey the foreign key
	 * @param equalComboBoxModel provides the combo box model controlling the equal value
	 * @param inSearchModel provides the search model controlling the in value
	 * @return a new {@link SwingForeignKeyConditionModel}
	 */
	public static SwingForeignKeyConditionModel swingForeignKeyConditionModel(ForeignKey foreignKey,
																																						Function<ForeignKey, EntityComboBoxModel> equalComboBoxModel,
																																						Function<ForeignKey, EntitySearchModel> inSearchModel) {
		return new SwingForeignKeyConditionModel(foreignKey, equalComboBoxModel, inSearchModel);
	}

	private void bindComboBoxEvents() {
		equalComboBoxModel.selectionEvent().addConsumer(new SetEqualValue());
		equalValue().addConsumer(new SelectEqualValue());
		equalComboBoxModel.refresher().refreshEvent().addListener(() -> equalComboBoxModel.setSelectedItem(getEqualValue()));
	}

	private void bindSearchModelEvents() {
		inSearchModel.entities().addConsumer(new SetInValues());
		inValues().addConsumer(new SelectInValues());
	}

	private final class SetEqualValue implements Consumer<Entity> {

		@Override
		public void accept(Entity selectedEntity) {
			if (!updatingModel) {
				setEqualValue(selectedEntity);
			}
		}
	}

	private final class SelectEqualValue implements Consumer<Entity> {

		@Override
		public void accept(Entity equalValue) {
			updatingModel = true;
			try {
				equalComboBoxModel.setSelectedItem(equalValue);
			}
			finally {
				updatingModel = false;
			}
		}
	}

	private final class SetInValues implements Consumer<Set<Entity>> {

		@Override
		public void accept(Set<Entity> selectedEntities) {
			if (!updatingModel) {
				setInValues(selectedEntities);
			}
		}
	}

	private final class SelectInValues implements Consumer<Set<Entity>> {

		@Override
		public void accept(Set<Entity> inValues) {
			updatingModel = true;
			try {
				inSearchModel.entities().set(inValues);
			}
			finally {
				updatingModel = false;
			}
		}
	}
}
