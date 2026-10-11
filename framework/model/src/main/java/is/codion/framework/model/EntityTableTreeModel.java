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
import is.codion.common.reactive.observer.Observable;
import is.codion.common.reactive.state.State;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.util.Collection;

/**
 * <p>The included items of an {@link EntityTableModel} as a {@link FilterTreeModel}, arranged by a foreign key
 * referencing the parent, from the table model entity type to itself.
 * <p>The tree follows the included items, reloaded synchronously after each change, so it shows the rows the table
 * model shows, siblings in the order of the rows. Sorting or filtering the table model does the same to the tree.
 * <p>The selection is kept in sync with the table model selection both ways, an entity selected in the table model,
 * or by item in the tree, having its ancestors expanded in the tree. The single selection mode of the tree follows the
 * one of the table model, so change the latter.
 * <p>An entity whose parent is not among the included items is an orphan, hidden along with everything below it, unless
 * shown as a root, see {@link #orphansAsRoots()}. An entity on a cycle of parents is reachable from no root, so neither
 * it nor anything below it is in the tree. The included items reachable from no root are available via
 * {@link #unreachable()}.
 * <p>Edit the table model rather than the tree, since a change made via {@link #nodes()} lasts only until the next
 * change to the included items.
 * <p>The table model holds the listeners of the tree model weakly, so it does not keep a tree model no longer in use,
 * one built for a dialog for example.
 * @see #builder()
 */
public interface EntityTableTreeModel extends FilterTreeModel<Entity> {

	/**
	 * Controls whether entities whose parent is not among the included items are shown as roots, otherwise they are
	 * hidden, along with everything below them, false by default.
	 * @return the {@link State} controlling whether orphans are shown as roots
	 */
	State orphansAsRoots();

	/**
	 * Controls whether siblings with children come before those without, in the order of the rows within each group,
	 * otherwise the siblings are in the order of the rows, false by default.
	 * @return the {@link State} controlling whether parents come first
	 */
	State parentsFirst();

	/**
	 * @return the {@link UnreachableItems}, the included items reachable from no root, so not in the tree
	 */
	UnreachableItems unreachable();

	/**
	 * @return a {@link Builder.TableModelStep} instance
	 */
	static Builder.TableModelStep builder() {
		return DefaultEntityTableTreeModel.DefaultBuilder.TABLE_MODEL;
	}

	/**
	 * <p>The included items reachable from no root, so not in the tree: the orphans not shown as roots, along with
	 * everything below them, and the entities on, or below, a cycle of parents.
	 * <p>Notified each time the tree has been rebuilt, following a change to the included items, to
	 * {@link #orphansAsRoots()} or to {@link #parentsFirst()}.
	 */
	interface UnreachableItems extends Observable<Collection<Entity>> {

		/**
		 * @return an unmodifiable collection of the unreachable items, empty in case the tree shows all the included items
		 */
		@Override
		Collection<Entity> get();
	}

	/**
	 * Builds a {@link EntityTableTreeModel}.
	 * @param <B> the builder type
	 */
	interface Builder<B extends Builder<B>> {

		/**
		 * Provides a {@link ParentStep}
		 */
		interface TableModelStep {

			/**
			 * @param tableModel the table model whose included items to arrange as a tree
			 * @return a {@link ParentStep}
			 */
			ParentStep tableModel(EntityTableModel<?, ?> tableModel);
		}

		/**
		 * Provides a {@link Builder}
		 */
		interface ParentStep {

			/**
			 * @param parent the foreign key referencing the parent, from the table model entity type to itself
			 * @return a {@link Builder}
			 * @throws IllegalArgumentException in case the foreign key does not reference the table model entity type
			 * from itself
			 */
			Builder<?> parent(ForeignKey parent);
		}

		/**
		 * @param orphansAsRoots the initial value, see {@link EntityTableTreeModel#orphansAsRoots()}
		 * @return this builder instance
		 */
		B orphansAsRoots(boolean orphansAsRoots);

		/**
		 * @param parentsFirst the initial value, see {@link EntityTableTreeModel#parentsFirst()}
		 * @return this builder instance
		 */
		B parentsFirst(boolean parentsFirst);

		/**
		 * @return a new {@link EntityTableTreeModel}
		 */
		EntityTableTreeModel build();
	}
}
