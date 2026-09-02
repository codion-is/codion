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
 * Copyright (c) 2008 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model.component;

import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.model.EntityComboBoxModel;
import is.codion.swing.common.model.component.combobox.SwingFilterComboBoxModel;

import java.util.function.Predicate;

/**
 * <p>A Swing {@link javax.swing.ComboBoxModel} based on an Entity — the Swing coat over the UI-agnostic
 * {@link EntityComboBoxModel}, adding the {@link SwingFilterComboBoxModel} interface (mirroring how
 * {@code SwingFilterComboBoxModel} relates to {@code FilterComboBoxModel}). All the entity logic (querying, filtering,
 * persistence-awareness) lives in {@link EntityComboBoxModel}; this only adds the Swing surface and a Swing-typed builder.
 * <p>To filter use {@link #filter()} to set a {@link Predicate} or configure {@link ForeignKey} based filtering.
 * @see #builder()
 * @see SwingFilterComboBoxModel#model(is.codion.common.model.component.combobox.FilterComboBoxModel)
 */
public interface SwingEntityComboBoxModel extends EntityComboBoxModel, SwingFilterComboBoxModel<Entity> {

	/**
	 * @return a {@link Builder.EntityTypeStep} instance
	 */
	static Builder.EntityTypeStep builder() {
		return DefaultSwingEntityComboBoxModel.DefaultBuilder.ENTITY_TYPE;
	}

	/**
	 * Builds a {@link SwingEntityComboBoxModel}, the {@link EntityComboBoxModel.Builder} options.
	 */
	interface Builder extends EntityComboBoxModel.Builder<Builder> {

		/**
		 * Specifies the entity type, either directly or derived from a {@link ForeignKey}.
		 * Provides a {@link ConnectionStep}
		 */
		interface EntityTypeStep extends EntityComboBoxModel.Builder.EntityTypeStep {

			@Override
			ConnectionStep entityType(EntityType entityType);

			@Override
			ConnectionStep foreignKey(ForeignKey foreignKey);
		}

		/**
		 * Provides a {@link Builder}
		 */
		interface ConnectionStep extends EntityComboBoxModel.Builder.ConnectionStep {

			@Override
			Builder connection(EntityConnection connection);
		}

		@Override
		SwingEntityComboBoxModel build();
	}
}
