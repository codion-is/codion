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
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.util.Set;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * A {@link is.codion.common.model.table.ColumnConditionModel} implementation based on a {@link EntitySearchModel}.
 * For instances use the {@link #entitySearchConditionModel(ForeignKey, EntitySearchModel)} factory method.
 * @see #entitySearchConditionModel(ForeignKey, EntitySearchModel)
 */
public final class EntitySearchConditionModel extends AbstractForeignKeyConditionModel {

	private final EntitySearchModel entitySearchModel;

	private boolean updatingModel = false;

	private EntitySearchConditionModel(ForeignKey foreignKey, EntitySearchModel entitySearchModel) {
		super(foreignKey);
		this.entitySearchModel = requireNonNull(entitySearchModel, "entitySearchModel");
		bindSearchModelEvents();
	}

	/**
	 * @return the {@link EntitySearchModel} used by this {@link EntitySearchConditionModel}
	 */
	public EntitySearchModel searchModel() {
		return entitySearchModel;
	}

	/**
	 * Instantiates a new {@link EntitySearchConditionModel} instance.
	 * @param foreignKey the foreign key
	 * @param entitySearchModel a EntitySearchModel
	 * @return a new {@link EntitySearchConditionModel} instance.
	 */
	public static EntitySearchConditionModel entitySearchConditionModel(ForeignKey foreignKey,
																																			EntitySearchModel entitySearchModel) {
		return new EntitySearchConditionModel(foreignKey, entitySearchModel);
	}

	private void bindSearchModelEvents() {
		entitySearchModel.entities().addDataListener(new EntitiesListener());
		equalValues().addDataListener(new EqualValuesListener());
	}

	private final class EntitiesListener implements Consumer<Set<Entity>> {

		@Override
		public void accept(Set<Entity> selectedEntities) {
			if (!updatingModel) {
				setEqualValues(selectedEntities);
			}
		}
	}

	private final class EqualValuesListener implements Consumer<Set<Entity>> {

		@Override
		public void accept(Set<Entity> equalValues) {
			updatingModel = true;
			try {
				entitySearchModel.entities().set(equalValues);
			}
			finally {
				updatingModel = false;
			}
		}
	}
}
