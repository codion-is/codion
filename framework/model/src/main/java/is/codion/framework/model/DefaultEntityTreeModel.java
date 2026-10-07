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
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.model.EntityTreeModel.Builder.Roots;

import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

final class DefaultEntityTreeModel extends AbstractEntityTreeModel<FilterTreeModel<Entity>> {

	DefaultEntityTreeModel(AbstractEntityTreeModelBuilder<?> builder) {
		super(builder, (roots, children, options) -> {
			FilterTreeModel.Builder<Entity, ?> treeModelBuilder = FilterTreeModel.builder()
							.roots(roots)
							.children(children);
			options.accept(treeModelBuilder);

			return treeModelBuilder.build();
		});
	}

	static final class DefaultBuilder extends AbstractEntityTreeModelBuilder<DefaultBuilder> {

		static final Builder.RootsStep ROOTS = new DefaultRootsStep();

		private DefaultBuilder(EntityType rootType, Consumer<Roots> roots, EntityConnection connection) {
			super(rootType, roots, connection);
		}
	}

	private static final class DefaultRootsStep implements Builder.RootsStep {

		@Override
		public Builder.ConnectionStep roots(EntityType entityType) {
			return roots(entityType, configuration -> {});
		}

		@Override
		public Builder.ConnectionStep roots(EntityType entityType, Consumer<Roots> roots) {
			return new DefaultConnectionStep(requireNonNull(entityType), requireNonNull(roots));
		}
	}

	private static final class DefaultConnectionStep implements Builder.ConnectionStep {

		private final EntityType rootType;
		private final Consumer<Roots> roots;

		private DefaultConnectionStep(EntityType rootType, Consumer<Roots> roots) {
			this.rootType = rootType;
			this.roots = roots;
		}

		@Override
		public Builder<?> connection(EntityConnection connection) {
			return new DefaultBuilder(rootType, roots, connection);
		}
	}
}
