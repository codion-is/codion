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
package is.codion.framework.model;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * A condition model using {@link EntitySearchModel} for
 * both the {@link #equalValue()} and {@link #inValues()}.
 * @see #foreignKeyConditionModel(ForeignKey, Function, Function)
 */
public final class ForeignKeyConditionModel extends AbstractForeignKeyConditionModel {

	private final EntitySearchModel equalSearchModel;
	private final EntitySearchModel inSearchModel;

	private boolean updatingModel = false;

	private ForeignKeyConditionModel(ForeignKey foreignKey,
																	 Function<ForeignKey, EntitySearchModel> equalSearchModel,
																	 Function<ForeignKey, EntitySearchModel> inSearchModel) {
		super(foreignKey);
		this.equalSearchModel = requireNonNull(equalSearchModel).apply(foreignKey);
		this.inSearchModel = requireNonNull(inSearchModel).apply(foreignKey);
		bindSearchModelEvents();
	}

	/**
	 * @return the combo box model controlling the equal value
	 */
	public EntitySearchModel equalSearchModel() {
		return equalSearchModel;
	}

	@Override
	public EntitySearchModel inSearchModel() {
		return inSearchModel;
	}

	public static ForeignKeyConditionModel foreignKeyConditionModel(ForeignKey foreignKey,
																																	Function<ForeignKey, EntitySearchModel> equalSearchModel,
																																	Function<ForeignKey, EntitySearchModel> inSearchModel) {
		return new ForeignKeyConditionModel(foreignKey, equalSearchModel, inSearchModel);
	}

	private void bindSearchModelEvents() {
		equalSearchModel.entity().addConsumer(new SetEqualValue());
		equalValue().addConsumer(new SelectEqualValue());
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

	private final class SetInValues implements Consumer<Set<Entity>> {

		@Override
		public void accept(Set<Entity> selectedEntities) {
			if (!updatingModel) {
				setInValues(selectedEntities);
			}
		}
	}

	private final class SelectEqualValue implements Consumer<Entity> {

		@Override
		public void accept(Entity equalValue) {
			updatingModel = true;
			try {
				equalSearchModel.entity().set(equalValue);
			}
			finally {
				updatingModel = false;
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
