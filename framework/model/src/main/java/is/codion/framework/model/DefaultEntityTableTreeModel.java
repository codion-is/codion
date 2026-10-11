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
package is.codion.framework.model;

import is.codion.common.model.component.tree.FilterTreeModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import static java.util.Objects.requireNonNull;

final class DefaultEntityTableTreeModel extends AbstractEntityTableTreeModel<FilterTreeModel<Entity>> {

	private DefaultEntityTableTreeModel(DefaultBuilder builder) {
		super(builder, (roots, children, options) -> {
			FilterTreeModel.Builder<Entity, ?> treeModelBuilder = FilterTreeModel.builder()
							.roots(roots)
							.children(children);
			options.accept(treeModelBuilder);

			return treeModelBuilder.build();
		});
	}

	static final class DefaultBuilder extends AbstractEntityTableTreeModelBuilder<DefaultBuilder> {

		static final Builder.TableModelStep TABLE_MODEL = new DefaultTableModelStep();

		private DefaultBuilder(EntityTableModel<?, ?> tableModel, ForeignKey parent) {
			super(tableModel, parent);
		}

		@Override
		public EntityTableTreeModel build() {
			return new DefaultEntityTableTreeModel(this);
		}
	}

	private static final class DefaultTableModelStep implements Builder.TableModelStep {

		@Override
		public Builder.ParentStep tableModel(EntityTableModel<?, ?> tableModel) {
			return new DefaultParentStep(requireNonNull(tableModel));
		}
	}

	private static final class DefaultParentStep implements Builder.ParentStep {

		private final EntityTableModel<?, ?> tableModel;

		private DefaultParentStep(EntityTableModel<?, ?> tableModel) {
			this.tableModel = tableModel;
		}

		@Override
		public Builder<?> parent(ForeignKey parent) {
			return new DefaultBuilder(tableModel, parent);
		}
	}
}
