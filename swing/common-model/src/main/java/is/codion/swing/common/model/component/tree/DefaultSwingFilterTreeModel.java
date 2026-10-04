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
package is.codion.swing.common.model.component.tree;

import is.codion.common.model.component.tree.AbstractFilterTreeModelBuilder;
import is.codion.common.model.component.tree.FilterTreeModel;
import is.codion.common.model.component.tree.FilterTreeSort;
import is.codion.common.model.component.tree.NodePath;

import org.jspecify.annotations.Nullable;

import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static is.codion.common.model.component.tree.NodePath.nodePath;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A Swing {@link javax.swing.tree.TreeModel} coat over the UI-agnostic {@link FilterTreeModel}: delegates the rich
 * model surface to a common instance, built with a {@link javax.swing.tree.TreeSelectionModel} based selection
 * ({@link DefaultTreeSelection}), and adds the {@link javax.swing.tree.TreeModel} methods, firing
 * {@link TreeModelEvent}s off the common model's {@link FilterTreeModel.NodesListener} notifications.
 */
final class DefaultSwingFilterTreeModel<T> implements SwingFilterTreeModel<T> {

	private final EventListenerList listeners = new EventListenerList();
	private final FilterTreeModel<T> model;
	private final DefaultTreeSelection<T> selection;

	private DefaultSwingFilterTreeModel(DefaultBuilder<T> builder) {
		this.model = builder.model(new TreeModelAdapter());
		this.selection = (DefaultTreeSelection<T>) model.selection();
	}

	@Override
	public Nodes<T> nodes() {
		return model.nodes();
	}

	@Override
	public Expansion<T> expansion() {
		return model.expansion();
	}

	@Override
	public VisibleNodes<T> visible() {
		return model.visible();
	}

	@Override
	public FilterTreeSelection<T> selection() {
		return selection;
	}

	@Override
	public FilterTreeSort<T> sort() {
		return model.sort();
	}

	@Override
	public NodePath<T> getRoot() {
		return nodePath();
	}

	@Override
	public NodePath<T> getChild(Object parent, int index) {
		return model.nodes().children(path(parent)).get(index);
	}

	@Override
	public int getChildCount(Object parent) {
		return model.nodes().children(path(parent)).size();
	}

	@Override
	public boolean isLeaf(Object node) {
		return model.nodes().leaf(path(node));
	}

	@Override
	public void valueForPathChanged(TreePath path, Object newValue) {
		throw new UnsupportedOperationException("Editing is not supported");
	}

	@Override
	public int getIndexOfChild(@Nullable Object parent, @Nullable Object child) {
		if (parent == null || child == null) {
			return -1;
		}

		return model.nodes().children(path(parent)).indexOf(child);
	}

	@Override
	public void addTreeModelListener(TreeModelListener listener) {
		listeners.add(TreeModelListener.class, requireNonNull(listener));
	}

	@Override
	public void removeTreeModelListener(TreeModelListener listener) {
		listeners.remove(TreeModelListener.class, requireNonNull(listener));
	}

	@Override
	public void fireNodesChanged(Collection<NodePath<T>> paths) {
		Map<NodePath<T>, List<Integer>> changed = new LinkedHashMap<>();
		for (NodePath<T> path : requireNonNull(paths)) {
			if (!path.root()) {
				int index = model.nodes().children(path.parent()).indexOf(path);
				if (index >= 0) {
					changed.computeIfAbsent(path.parent(), parent -> new ArrayList<>()).add(index);
				}
			}
		}
		changed.forEach((parent, indexes) -> {
			List<Integer> sorted = indexes.stream()
							.distinct()
							.sorted()
							.collect(toList());
			fire(TreeModelListener::treeNodesChanged, event(parent, sorted, children(parent, sorted)));
		});
	}

	@Override
	public TreePath treePath(NodePath<T> path) {
		return createTreePath(requireNonNull(path));
	}

	static TreePath createTreePath(NodePath<?> path) {
		Object[] components = new Object[path.depth() + 1];
		NodePath<?> component = path;
		for (int i = components.length - 1; i >= 0; i--) {
			components[i] = component;
			if (i > 0) {
				component = component.parent();
			}
		}

		return new TreePath(components);
	}

	private Object[] children(NodePath<T> parent, List<Integer> indexes) {
		List<NodePath<T>> children = model.nodes().children(parent);
		Object[] objects = new Object[indexes.size()];
		for (int i = 0; i < objects.length; i++) {
			objects[i] = children.get(indexes.get(i));
		}

		return objects;
	}

	private TreeModelEvent event(NodePath<T> parent, List<Integer> indexes, Object[] children) {
		//TreeModelEvent takes the indexes as an int array
		int[] childIndices = new int[indexes.size()];
		for (int i = 0; i < childIndices.length; i++) {
			childIndices[i] = indexes.get(i);
		}

		return new TreeModelEvent(this, createTreePath(parent), childIndices, children);
	}

	private void fire(BiConsumer<TreeModelListener, TreeModelEvent> notification, TreeModelEvent event) {
		boolean wasStructural = selection.structural(true);
		try {
			TreeModelListener[] treeModelListeners = listeners.getListeners(TreeModelListener.class);
			//getListeners() returns the last added first, notified in the order they were added
			for (int i = treeModelListeners.length - 1; i >= 0; i--) {
				notification.accept(treeModelListeners[i], event);
			}
		}
		finally {
			selection.structural(wasStructural);
		}
	}

	private static <T> NodePath<T> path(Object node) {
		return (NodePath<T>) requireNonNull(node);
	}

	private final class TreeModelAdapter implements NodesListener<T> {

		@Override
		public void inserted(NodePath<T> parent, List<Integer> indexes) {
			fire(TreeModelListener::treeNodesInserted, event(parent, indexes, children(parent, indexes)));
		}

		@Override
		public void removed(NodePath<T> parent, List<Integer> indexes, List<NodePath<T>> children) {
			fire(TreeModelListener::treeNodesRemoved, event(parent, indexes, children.toArray()));
		}

		@Override
		public void changed(NodePath<T> parent, List<Integer> indexes) {
			fire(TreeModelListener::treeNodesChanged, event(parent, indexes, children(parent, indexes)));
		}

		@Override
		public void structureChanged(NodePath<T> path) {
			fire(TreeModelListener::treeStructureChanged, new TreeModelEvent(DefaultSwingFilterTreeModel.this, createTreePath(path)));
		}
	}

	private static final class DefaultRootsStep implements Builder.RootsStep {

		@Override
		public <T> Builder.ChildrenStep<T> roots(Supplier<Collection<T>> roots) {
			return new DefaultChildrenStep<>(requireNonNull(roots));
		}
	}

	private static final class DefaultChildrenStep<T> implements Builder.ChildrenStep<T> {

		private final Supplier<Collection<T>> roots;

		private DefaultChildrenStep(Supplier<Collection<T>> roots) {
			this.roots = roots;
		}

		@Override
		public Builder<T> children(Function<NodePath<T>, Collection<T>> children) {
			return new DefaultBuilder<>(roots, requireNonNull(children));
		}
	}

	static final class DefaultBuilder<T> extends AbstractFilterTreeModelBuilder<T, Builder<T>> implements Builder<T> {

		static final Builder.RootsStep ROOTS = new DefaultRootsStep();

		private DefaultBuilder(Supplier<Collection<T>> roots, Function<NodePath<T>, Collection<T>> children) {
			super(roots, children);
		}

		@Override
		public SwingFilterTreeModel<T> build() {
			SwingFilterTreeModel<T> model = new DefaultSwingFilterTreeModel<>(this);
			if (refresh()) {
				model.nodes().refresh();
			}

			return model;
		}

		FilterTreeModel<T> model(NodesListener<T> adapter) {
			return build(DefaultTreeSelection::new, adapter);
		}
	}
}
