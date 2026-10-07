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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model.component;

import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.model.EntityTreeModel;
import is.codion.swing.common.model.component.tree.SwingFilterTreeModel;

import java.util.function.Consumer;

/**
 * A Swing {@link EntityTreeModel}, a {@link SwingFilterTreeModel} of entities, for a
 * {@code is.codion.swing.common.ui.component.tree.FilterTree}.
 * @see #builder()
 */
public interface SwingEntityTreeModel extends EntityTreeModel, SwingFilterTreeModel<Entity> {

	/**
	 * @return a {@link Builder.RootsStep} instance
	 */
	static Builder.RootsStep builder() {
		return DefaultSwingEntityTreeModel.DefaultBuilder.ROOTS;
	}

	/**
	 * Builds a {@link SwingEntityTreeModel}
	 */
	interface Builder extends EntityTreeModel.Builder<Builder> {

		/**
		 * Provides a {@link ConnectionStep}
		 */
		interface RootsStep extends EntityTreeModel.Builder.RootsStep {

			@Override
			ConnectionStep roots(EntityType entityType);

			@Override
			ConnectionStep roots(EntityType entityType, Consumer<Roots> roots);
		}

		/**
		 * Provides a {@link Builder}
		 */
		interface ConnectionStep extends EntityTreeModel.Builder.ConnectionStep {

			@Override
			Builder connection(EntityConnection connection);
		}

		@Override
		SwingEntityTreeModel build();
	}
}
