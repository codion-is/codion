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
import is.codion.common.model.component.tree.FilterTreeSort;
import is.codion.common.model.component.tree.NodePath;
import is.codion.common.model.component.tree.TreeSelection;
import is.codion.common.reactive.state.State;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static is.codion.common.model.component.tree.NodePath.nodePath;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * <p>A base class for {@link EntityTableTreeModel} implementations, based on a tree model built by a toolkit, a
 * {@code javax.swing.tree.TreeModel} based one for example, the logic, arranging the rows and keeping the selections in
 * sync, living here.
 * <p>The {@link FilterTreeModel} methods delegate to the tree model, see {@link #treeModel()}.
 * @param <M> the type of the underlying tree model
 * @see AbstractEntityTableTreeModelBuilder
 */
public abstract class AbstractEntityTableTreeModel<M extends FilterTreeModel<Entity>> implements EntityTableTreeModel {

	private final EntityTableModel<?, ?> tableModel;
	private final ForeignKey parent;
	private final State orphansAsRoots;
	private final State parentsFirst;
	private final M treeModel;

	//we keep references to these listeners, since the table model only holds them weakly
	private final Runnable itemsListener = this::rebuild;
	private final Consumer<List<Entity>> selectionListener = this::tableSelectionChanged;
	private final Consumer<Boolean> singleSelectionListener = this::singleSelectionChanged;

	//the children by parent, built from the included items when needed, null after they change
	private @Nullable Index index;
	//true while one selection is being synchronized with the other, or the tree with the items
	private boolean synchronizing = false;

	/**
	 * @param builder the builder
	 * @param treeModel builds the underlying tree model
	 */
	protected AbstractEntityTableTreeModel(AbstractEntityTableTreeModelBuilder<?> builder, TreeModelBuilder<M> treeModel) {
		this.tableModel = requireNonNull(builder).tableModel;
		this.parent = builder.parent;
		this.orphansAsRoots = State.state(builder.orphansAsRoots);
		this.parentsFirst = State.state(builder.parentsFirst);
		this.treeModel = requireNonNull(requireNonNull(treeModel).build(() -> index().roots,
						path -> index().children(path.item()),
						treeModelBuilder -> treeModelBuilder.leaf(path -> index().children(path.item()).isEmpty())));
		// in memory, from the included items
		this.treeModel.nodes().loader().async().set(false);
		this.treeModel.selection().singleSelection().set(tableModel.selection().singleSelection().is());
		this.treeModel.selection().items().addConsumer(this::treeSelectionChanged);
		this.orphansAsRoots.addListener(this::rebuild);
		this.parentsFirst.addListener(this::rebuild);
		tableModel.items().included().addWeakListener(itemsListener);
		tableModel.selection().items().addWeakConsumer(selectionListener);
		tableModel.selection().singleSelection().addWeakConsumer(singleSelectionListener);
		rebuild();
	}

	@Override
	public final State orphansAsRoots() {
		return orphansAsRoots;
	}

	@Override
	public final State parentsFirst() {
		return parentsFirst;
	}

	@Override
	public final Nodes<Entity> nodes() {
		return treeModel.nodes();
	}

	@Override
	public final Expansion<Entity> expansion() {
		return treeModel.expansion();
	}

	@Override
	public final VisibleNodes<Entity> visible() {
		return treeModel.visible();
	}

	@Override
	public TreeSelection<Entity> selection() {
		return treeModel.selection();
	}

	@Override
	public final FilterTreeSort<Entity> sort() {
		return treeModel.sort();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [parent: " + parent + "]";
	}

	/**
	 * @return the underlying tree model
	 */
	protected final M treeModel() {
		return treeModel;
	}

	private Index index() {
		if (index == null) {
			index = new Index(tableModel.items().included().get(), parent, orphansAsRoots.is(), parentsFirst.is());
		}

		return index;
	}

	private void rebuild() {
		index = null;
		boolean wasSynchronizing = synchronizing;
		synchronizing = true;
		try {
			treeModel.nodes().refresh();
			// the tree may have dropped a moved node from its selection, and the table selection may have changed
			// before the items notified, while the index was out of date
			select(tableModel.selection().items().get());
		}
		finally {
			synchronizing = wasSynchronizing;
		}
	}

	private void treeSelectionChanged(List<Entity> entities) {
		synchronize(() -> tableModel.selection().items().set(entities));
	}

	private void tableSelectionChanged(List<Entity> entities) {
		synchronize(() -> select(entities));
	}

	private void singleSelectionChanged(boolean singleSelection) {
		// both selections are cleared when the mode changes
		treeModel.selection().singleSelection().set(singleSelection);
	}

	private void synchronize(Runnable synchronization) {
		if (!synchronizing) {
			synchronizing = true;
			try {
				synchronization.run();
			}
			finally {
				synchronizing = false;
			}
		}
	}

	private void select(List<Entity> entities) {
		List<NodePath<Entity>> paths = new ArrayList<>(entities.size());
		for (Entity entity : entities) {
			NodePath<Entity> path = index().path(entity);
			if (path != null) {
				NodePath<Entity> parentPath = path.parent();
				if (!parentPath.root()) {
					treeModel.expansion().expand(parentPath);
				}
				paths.add(path);
			}
		}
		treeModel.selection().paths().set(paths);
	}

	/**
	 * Builds the underlying tree model of an {@link EntityTableTreeModel}.
	 * @param <M> the tree model type
	 */
	protected interface TreeModelBuilder<M extends FilterTreeModel<Entity>> {

		/**
		 * Builds the tree model, given the roots supplier and children function, the tree model builder being
		 * configured via the given options before it builds the model.
		 * @param roots supplies the roots
		 * @param children provides the children of a node
		 * @param options configures the tree model builder with the table tree defaults
		 * @return a new tree model
		 */
		M build(Supplier<Collection<Entity>> roots, Function<NodePath<Entity>, Collection<Entity>> children,
						Consumer<FilterTreeModel.Builder<Entity, ?>> options);
	}

	private static final class Index {

		private final ForeignKey parent;
		private final boolean orphansAsRoots;
		private final Map<Entity.Key, Entity> entities;
		private final Map<Entity.Key, List<Entity>> children = new HashMap<>();
		private final List<Entity> roots;

		private Index(List<Entity> items, ForeignKey parent, boolean orphansAsRoots, boolean parentsFirst) {
			this.parent = parent;
			this.orphansAsRoots = orphansAsRoots;
			this.entities = new HashMap<>(items.size());
			List<Entity> distinct = new ArrayList<>(items.size());
			for (Entity entity : items) {
				// the first of equal items, siblings must be distinct
				if (entities.putIfAbsent(entity.primaryKey(), entity) == null) {
					distinct.add(entity);
				}
			}
			List<Entity> topLevel = new ArrayList<>();
			for (Entity entity : distinct) {
				Entity.Key parentKey = entity.key(parent);
				if (parentKey == null || (orphansAsRoots && !entities.containsKey(parentKey))) {
					topLevel.add(entity);
				}
				else {
					children.computeIfAbsent(parentKey, key -> new ArrayList<>()).add(entity);
				}
			}
			if (parentsFirst) {
				roots = parentsFirst(topLevel);
				children.replaceAll((key, siblings) -> parentsFirst(siblings));
			}
			else {
				roots = topLevel;
			}
		}

		private List<Entity> children(Entity entity) {
			return children.getOrDefault(entity.primaryKey(), emptyList());
		}

		/**
		 * @return the path of the given entity in the tree, null in case it is not in the tree, being on, or below,
		 * a cycle of parents or an orphan not shown as a root, or not among the items
		 */
		private @Nullable NodePath<Entity> path(Entity entity) {
			List<Entity> path = new ArrayList<>();
			Entity ancestor = entities.get(entity.primaryKey());
			//bounded, in case of a cycle
			while (ancestor != null && path.size() < entities.size()) {
				path.add(ancestor);
				Entity.Key parentKey = ancestor.key(parent);
				ancestor = parentKey == null ? null : entities.get(parentKey);
				if (ancestor == null && (parentKey == null || orphansAsRoots)) {
					Collections.reverse(path);

					return nodePath(path);
				}
			}

			return null;
		}

		/**
		 * @return the given siblings, those with children first, in the given order within each group
		 */
		private List<Entity> parentsFirst(List<Entity> siblings) {
			List<Entity> ordered = new ArrayList<>(siblings.size());
			Predicate<Entity> hasChildren = sibling -> children.containsKey(sibling.primaryKey());
			siblings.stream().filter(hasChildren).forEach(ordered::add);
			siblings.stream().filter(hasChildren.negate()).forEach(ordered::add);

			return ordered;
		}
	}
}
