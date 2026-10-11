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

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.model.EntityTableModel;
import is.codion.framework.model.EntityTableTreeModel;
import is.codion.swing.common.model.component.tree.SwingFilterTreeModel;

/**
 * A Swing {@link EntityTableTreeModel}, a {@link SwingFilterTreeModel} of entities, for a
 * {@code is.codion.swing.common.ui.component.tree.FilterTree}.
 * @see #builder()
 */
public interface SwingEntityTableTreeModel extends EntityTableTreeModel, SwingFilterTreeModel<Entity> {

	/**
	 * @return a {@link Builder.TableModelStep} instance
	 */
	static Builder.TableModelStep builder() {
		return DefaultSwingEntityTableTreeModel.DefaultBuilder.TABLE_MODEL;
	}

	/**
	 * Builds a {@link SwingEntityTableTreeModel}
	 */
	interface Builder extends EntityTableTreeModel.Builder<Builder> {

		/**
		 * Provides a {@link ParentStep}
		 */
		interface TableModelStep extends EntityTableTreeModel.Builder.TableModelStep {

			@Override
			ParentStep tableModel(EntityTableModel<?, ?> tableModel);
		}

		/**
		 * Provides a {@link Builder}
		 */
		interface ParentStep extends EntityTableTreeModel.Builder.ParentStep {

			@Override
			Builder parent(ForeignKey parent);
		}

		@Override
		SwingEntityTableTreeModel build();
	}
}
