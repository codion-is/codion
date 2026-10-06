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
package is.codion.common.model.component.tree;

import is.codion.common.model.CancelException;
import is.codion.common.model.component.tree.AbstractFilterTreeModelBuilder.SelectionContext;
import is.codion.common.model.filter.FilterModel;
import is.codion.common.model.filter.FilterModel.IncludePredicate;
import is.codion.common.model.filter.SortOrder;
import is.codion.common.model.selection.MultiSelection;
import is.codion.common.model.worker.ProgressWorker;
import is.codion.common.model.worker.ProgressWorker.ResultTaskHandler;
import is.codion.common.reactive.event.Event;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.AbstractValue;
import is.codion.common.reactive.value.Value;
import is.codion.common.utilities.dispatch.Dispatcher;
import is.codion.common.utilities.exceptions.Exceptions;

import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static is.codion.common.model.component.tree.NodePath.nodePath;
import static is.codion.common.reactive.value.Value.Notify.SET;
import static is.codion.common.utilities.Nulls.rejectNulls;
import static java.util.Collections.*;
import static java.util.Comparator.comparingInt;
import static java.util.Objects.requireNonNull;

final class DefaultFilterTreeModel<T> implements FilterTreeModel<T> {

	private static final String SIBLINGS_MUST_BE_DISTINCT = "Tree node siblings must be distinct: ";

	private final Supplier<Collection<T>> roots;
	private final Function<NodePath<T>, Collection<T>> children;
	private final @Nullable Function<List<NodePath<T>>, Collection<NodePath<T>>> leaves;
	private final @Nullable NodesListener<T> listener;
	private final Consumer<Exception> onLoadException;

	//every node in the model, the root included, by path
	private final Map<NodePath<T>, Node<T>> nodeMap = new HashMap<>();
	private final Node<T> root;
	//the expanded paths, the ones remembered below collapsed nodes included
	private final Set<NodePath<T>> expanded = new LinkedHashSet<>();

	private final DefaultNodes nodes = new DefaultNodes();
	private final DefaultLoader loader = new DefaultLoader();
	private final DefaultExpansion expansion = new DefaultExpansion();
	private final DefaultVisibleNodes visible = new DefaultVisibleNodes();
	private final DefaultIncludePredicate<T> predicate = new DefaultIncludePredicate<>();
	private final DefaultSort sort;
	private final TreeSelection<T> selection;

	//the groups of children touched by the current mutation, with their state before it, see touch()
	private final Map<Node<T>, Snapshot<T>> snapshots = new LinkedHashMap<>();
	private final List<NodePath<T>> pendingExpanded = new ArrayList<>();
	private final List<NodePath<T>> pendingCollapsed = new ArrayList<>();

	//true while a mutation is in progress, see mutate(). Like the flat model this assumes mutations happen on a
	//single thread, the UI thread, where load results are delivered
	private boolean mutating = false;
	//true when a mutation was made while a mutation was being committed, by a listener reacting to it
	private boolean recommit = false;
	private boolean filterRequested = false;
	//true when nodes have been added, removed or had their items replaced by the current mutation
	private boolean nodesChanged = false;
	private long sequence = 0;
	//the path of the node to select once visible, null when none is pending, see TreeSelection.set(NodePath)
	private @Nullable NodePath<T> pendingSelection;
	//true while the pending selection is being made, so that its selection change does not cancel it
	private boolean selectingPending = false;

	DefaultFilterTreeModel(AbstractFilterTreeModelBuilder<T, ?> builder,
												 Function<SelectionContext<T>, TreeSelection<T>> selectionFactory,
												 @Nullable NodesListener<T> listener) {
		this.roots = builder.roots;
		this.children = builder.children;
		this.leaves = builder.leaves;
		this.listener = listener;
		this.onLoadException = builder.onLoadException == null ? new RethrowExceptionHandler() : builder.onLoadException;
		this.sort = new DefaultSort(builder.comparator);
		this.root = new Node<>(nodePath(), null, true);
		this.nodeMap.put(root.path, root);
		this.selection = selectionFactory.apply(new DefaultSelectionContext());
		builder.selectionListeners.forEach(selection.indexes()::addListener);
		builder.itemSelectedListeners.forEach(selection.item()::addConsumer);
		builder.itemsSelectedListeners.forEach(selection.items()::addConsumer);
		builder.pathSelectedListeners.forEach(selection.path()::addConsumer);
		builder.pathsSelectedListeners.forEach(selection.paths()::addConsumer);
		builder.indexSelectedListeners.forEach(selection.index()::addConsumer);
		builder.indexesSelectedListeners.forEach(selection.indexes()::addConsumer);
		this.predicate.set(builder.included);
		this.predicate.addListener(this::filter);
		this.sort.changed.addListener(this::sortChanged);
		//a selection change from outside, by the application or the user, cancels a pending selection,
		//the model restoring the selection after a mutation not notifying changing()
		this.selection.changing().addListener(this::cancelPendingSelection);
	}

	@Override
	public Nodes<T> nodes() {
		return nodes;
	}

	@Override
	public Expansion<T> expansion() {
		return expansion;
	}

	@Override
	public VisibleNodes<T> visible() {
		return visible;
	}

	@Override
	public TreeSelection<T> selection() {
		return selection;
	}

	@Override
	public FilterTreeSort<T> sort() {
		return sort;
	}

	private void filter() {
		mutate(() -> filterRequested = true);
	}

	private void sortChanged() {
		mutate(() -> nodeMap.values().stream()
						.filter(node -> node.children != null)
						.forEach(this::touch));
	}

	/**
	 * Runs the given mutation, preserving the selection by path, see {@link #mutate(Runnable, UnaryOperator)}.
	 * @param mutation the mutation
	 */
	private void mutate(Runnable mutation) {
		mutate(mutation, UnaryOperator.identity());
	}

	/**
	 * <p>Runs the given mutation, then commits it: evaluates the inclusion, rebuilds the included children of the
	 * groups touched, recomputes the visible nodes and notifies the {@link NodesListener}, before restoring the
	 * selection by path, all with the selection grouped, so that no listener sees the selection the mutation
	 * momentarily leaves behind.
	 * <p>The visible nodes are notified once the selection has been restored, followed by the expansion events, and
	 * finally the loading of expanded nodes not yet loaded is started.
	 * <p>A mutation made while one is in progress, by a listener reacting to it, becomes part of it.
	 * @param mutation the mutation
	 * @param mapping maps the selected paths to the ones to restore, for a mutation changing paths
	 */
	private void mutate(Runnable mutation, UnaryOperator<NodePath<T>> mapping) {
		if (mutating) {
			mutation.run();
			recommit = true;
			return;
		}
		List<NodePath<T>> selected = selection.paths().get();
		boolean selectionGrouping = selection.grouping().is();
		selection.grouping().set(true);
		mutating = true;
		try {
			mutation.run();
			commit();
			selection.paths().restore(restore(selected, mapping));
			while (recommit || !snapshots.isEmpty()) {
				//a mutation made by a selection listener, while the selection was being restored
				List<NodePath<T>> restored = selection.paths().get();
				commit();
				selection.paths().restore(restore(restored, UnaryOperator.identity()));
			}
		}
		finally {
			mutating = false;
			try {
				selection.grouping().set(selectionGrouping);
			}
			finally {
				//the mutation has been made, whether or not a selection listener threw, vetoing the selection change
				//following it for example, in which case the views must still be told of it
				visible.notifyPending();
				notifyExpansion();
			}
		}
		loadExpanded();
		//the mutation may have cancelled loads
		loader.updateActive();
		resolvePendingSelection();
	}

	/**
	 * Selects the node identified by the given path once it is visible, see {@link TreeSelection#set(NodePath)}.
	 */
	private void selectWhenVisible(NodePath<T> path) {
		if (path.root()) {
			throw new IllegalArgumentException("The root can not be selected");
		}
		pendingSelection = path;
		expansion.expand(path.parent());
		resolvePendingSelection();
	}

	/**
	 * Resolves the pending selection, in case the model shows how far its path goes: selects the node in case it
	 * exists, otherwise the deepest node on the path which does, once it is loaded, or neither loaded nor being loaded,
	 * so that the rest of the path does not exist or can not be loaded. Pending while a load at or above that node is
	 * in progress, which may bring the rest of the path.
	 */
	private void resolvePendingSelection() {
		NodePath<T> path = pendingSelection;
		if (path == null) {
			return;
		}
		NodePath<T> stopped = path;
		while (!nodeMap.containsKey(stopped)) {
			stopped = stopped.parent();
		}
		if (!stopped.equals(path) && loader.loading(stopped)) {
			return;
		}
		pendingSelection = null;
		selectResolved(stopped);
	}

	/**
	 * Selects the given node, or its nearest visible ancestor, leaving the selection as is in case of none.
	 */
	private void selectResolved(NodePath<T> path) {
		NodePath<T> visiblePath = path;
		while (!visiblePath.root() && visible.indexOf(visiblePath) < 0) {
			visiblePath = visiblePath.parent();
		}
		if (visiblePath.root()) {
			return;
		}
		selectingPending = true;
		try {
			selection.paths().set(singletonList(visiblePath));
		}
		catch (CancelException e) {
			//vetoed, the pending selection ends unselected
		}
		finally {
			selectingPending = false;
		}
	}

	private void cancelPendingSelection() {
		if (!selectingPending) {
			pendingSelection = null;
		}
	}

	/**
	 * Expands the ancestors of the nodes identified by the given paths which are in the model and included but hidden
	 * below a collapsed ancestor, so that they can be selected, ancestors before descendants, in a single mutation.
	 */
	private void expandHidden(Collection<NodePath<T>> paths) {
		Set<NodePath<T>> toExpand = new LinkedHashSet<>();
		for (NodePath<T> path : paths) {
			Node<T> node = nodeMap.get(path);
			if (node != null && node != root && node.included && visible.indexOf(path) < 0) {
				NodePath<T> ancestor = path.parent();
				while (!ancestor.root()) {
					if (!expanded.contains(ancestor)) {
						toExpand.add(ancestor);
					}
					ancestor = ancestor.parent();
				}
			}
		}
		if (!toExpand.isEmpty()) {
			List<NodePath<T>> ancestors = new ArrayList<>(toExpand);
			ancestors.sort(comparingInt(NodePath::depth));
			mutate(() -> {
				expanded.addAll(ancestors);
				pendingExpanded.addAll(ancestors);
			});
		}
	}

	/**
	 * Returns the paths of the included nodes holding any of the given items, depth first, in the order the nodes are
	 * shown once their ancestors are expanded.
	 */
	private List<NodePath<T>> occurrences(Set<T> items) {
		List<NodePath<T>> occurrences = new ArrayList<>();
		collectOccurrences(root, items, occurrences);

		return occurrences;
	}

	private void collectOccurrences(Node<T> node, Set<T> items, List<NodePath<T>> occurrences) {
		for (Node<T> child : node.includedChildren) {
			if (items.contains(child.path.item())) {
				occurrences.add(child.path);
			}
			collectOccurrences(child, items, occurrences);
		}
	}

	private void commit() {
		do {
			recommit = false;
			if (filterRequested || (nodesChanged && predicate.get() != null)) {
				include(root, filterRequested);
			}
			filterRequested = false;
			nodesChanged = false;
			Map<Node<T>, Snapshot<T>> touched = new LinkedHashMap<>(snapshots);
			snapshots.clear();
			touched.keySet().forEach(this::rebuild);
			visible.update();
			notifyListener(touched);
		}
		while (recommit || !snapshots.isEmpty());
	}

	/**
	 * Records the state of the given group of children before it changes, once per mutation.
	 * Must be called before the children of the node, or their inclusion, are changed.
	 * @param node the node whose children are about to change
	 */
	private void touch(Node<T> node) {
		if (!snapshots.containsKey(node)) {
			snapshots.put(node, new Snapshot<>(node.includedPaths, leaf(node)));
		}
	}

	/**
	 * Evaluates the inclusion of the given node and its loaded descendants, touching the parents of nodes whose
	 * inclusion changes.
	 * @param node the node
	 * @param all true if all the nodes should be tested against the predicate, false if only the ones not tested
	 * since they were added or had their item replaced
	 * @return true if the node is included
	 */
	private boolean include(Node<T> node, boolean all) {
		boolean includedChild = false;
		if (node.children != null) {
			for (Node<T> child : node.children) {
				if (include(child, all)) {
					includedChild = true;
				}
			}
		}
		if (node == root) {
			return true;
		}
		if (all || node.passes == null) {
			node.passes = predicate.test(node.path);
		}
		boolean included = includedChild || node.passes;
		if (included != node.included) {
			touch(requireNonNull(node.parent));
			node.included = included;
		}

		return included;
	}

	/**
	 * Rebuilds the included children of the given node from its children, in order, sorted when sorting.
	 * @param node the node
	 */
	private void rebuild(Node<T> node) {
		if (node.children == null || nodeMap.get(node.path) != node) {
			return;
		}
		List<Node<T>> included = new ArrayList<>(node.children.size());
		for (Node<T> child : node.children) {
			if (child.included) {
				included.add(child);
			}
		}
		if (sort.sorted()) {
			included.sort((child, other) -> sort.compare(child.path.item(), other.path.item()));
		}
		List<NodePath<T>> paths = new ArrayList<>(included.size());
		for (Node<T> child : included) {
			paths.add(child.path);
		}
		node.included(included, unmodifiableList(paths));
	}

	private boolean leaf(Node<T> node) {
		if (node == root) {
			return false;
		}
		if (node.children == null) {
			return node.leaf;
		}

		return node.includedChildren.isEmpty();
	}

	/**
	 * Sets the leaf status the leaves function reported for the given node, touching it in case it changed, unless a
	 * later load, add or replace has already set it, or the node has been loaded meanwhile, a loaded node being a leaf
	 * when it has no included children, whatever the leaves function reports.
	 * @param node the node
	 * @param leaf the leaf status
	 * @param sequence the load, add or replace calling the leaves function
	 */
	private void leaf(Node<T> node, boolean leaf, long sequence) {
		if (node.children != null || node.leafSequence > sequence) {
			return;
		}
		if (node.leaf != leaf) {
			touch(node);
			node.leaf = leaf;
		}
		node.leafSequence = sequence;
	}

	/**
	 * Called off the UI thread when loading asynchronously.
	 * @param path the path
	 * @return true if the leaves function reports the node identified by the given path as a leaf, false without one
	 */
	private boolean testLeaf(NodePath<T> path) {
		return testLeaves(singletonList(path)).contains(path);
	}

	/**
	 * Called off the UI thread when loading asynchronously.
	 * @param paths the paths
	 * @return the given paths which the leaves function reports as leaves, an empty set without one
	 */
	private Set<NodePath<T>> testLeaves(List<NodePath<T>> paths) {
		if (leaves == null || paths.isEmpty()) {
			return emptySet();
		}
		Set<NodePath<T>> leafPaths = new HashSet<>(requireNonNull(leaves.apply(unmodifiableList(paths)),
						"The leaves function may not return null"));
		leafPaths.retainAll(paths);

		return leafPaths;
	}

	private List<NodePath<T>> restore(List<NodePath<T>> selected, UnaryOperator<NodePath<T>> mapping) {
		List<NodePath<T>> restored = new ArrayList<>(selected.size());
		for (NodePath<T> selectedPath : selected) {
			NodePath<T> path = mapping.apply(selectedPath);
			if (visible.indexOf(path) >= 0) {
				restored.add(path);
			}
			else {
				Node<T> node = nodeMap.get(path);
				if (node != null && node.included) {
					//hidden by a collapsed ancestor, replaced by the nearest visible one
					NodePath<T> ancestor = path.parent();
					while (!ancestor.root() && visible.indexOf(ancestor) < 0) {
						ancestor = ancestor.parent();
					}
					if (!ancestor.root()) {
						restored.add(ancestor);
					}
				}
			}
		}

		return restored;
	}

	private void notifyListener(Map<Node<T>, Snapshot<T>> touched) {
		if (listener == null || touched.isEmpty()) {
			return;
		}
		List<Map.Entry<Node<T>, Snapshot<T>>> groups = new ArrayList<>(touched.entrySet());
		groups.sort(comparingInt(entry -> entry.getKey().path.depth()));
		List<NodePath<T>> structureChanged = new ArrayList<>();
		for (Map.Entry<Node<T>, Snapshot<T>> group : groups) {
			Node<T> node = group.getKey();
			if (nodeMap.get(node.path) == node && known(node, touched) && !covered(node.path, structureChanged)) {
				notifyLeaf(node, group.getValue());
				notifyChildren(node, group.getValue().paths, structureChanged);
			}
		}
	}

	/**
	 * @return true if the node was reachable from the root via included nodes both before and after the mutation,
	 * a view knowing about its children
	 */
	private boolean known(Node<T> node, Map<Node<T>, Snapshot<T>> touched) {
		Node<T> child = node;
		Node<T> parent = node.parent;
		while (parent != null) {
			Snapshot<T> snapshot = touched.get(parent);
			List<NodePath<T>> before = snapshot == null ? parent.includedPaths : snapshot.paths;
			if (!before.contains(child.path) || !parent.includedPaths.contains(child.path)) {
				return false;
			}
			child = parent;
			parent = parent.parent;
		}

		return true;
	}

	private static <T> boolean covered(NodePath<T> path, List<NodePath<T>> structureChanged) {
		for (NodePath<T> changed : structureChanged) {
			if (changed.contains(path)) {
				return true;
			}
		}

		return false;
	}

	private void notifyLeaf(Node<T> node, Snapshot<T> snapshot) {
		Node<T> parent = node.parent;
		if (parent != null && snapshot.leaf != leaf(node)) {
			int index = parent.includedPaths.indexOf(node.path);
			if (index >= 0) {
				requireNonNull(listener).changed(parent.path, singletonList(index));
			}
		}
	}

	private void notifyChildren(Node<T> node, List<NodePath<T>> before, List<NodePath<T>> structureChanged) {
		List<NodePath<T>> after = node.includedPaths;
		if (before == after) {
			return;
		}
		NodesListener<T> nodesListener = requireNonNull(listener);
		Set<NodePath<T>> beforeSet = new HashSet<>(before);
		Set<NodePath<T>> afterSet = new HashSet<>(after);
		List<Integer> removedIndexes = new ArrayList<>();
		List<NodePath<T>> removed = new ArrayList<>();
		List<NodePath<T>> retainedBefore = new ArrayList<>(before.size());
		for (int i = 0; i < before.size(); i++) {
			if (afterSet.contains(before.get(i))) {
				retainedBefore.add(before.get(i));
			}
			else {
				removedIndexes.add(i);
				removed.add(before.get(i));
			}
		}
		List<Integer> insertedIndexes = new ArrayList<>();
		List<Integer> retainedIndexes = new ArrayList<>(after.size());
		List<NodePath<T>> retainedAfter = new ArrayList<>(after.size());
		for (int i = 0; i < after.size(); i++) {
			if (beforeSet.contains(after.get(i))) {
				retainedIndexes.add(i);
				retainedAfter.add(after.get(i));
			}
			else {
				insertedIndexes.add(i);
			}
		}
		if ((!removed.isEmpty() && !insertedIndexes.isEmpty()) || !retainedBefore.equals(retainedAfter)) {
			//both removed and inserted, or reordered
			nodesListener.structureChanged(node.path);
			structureChanged.add(node.path);
			return;
		}
		if (!removed.isEmpty()) {
			nodesListener.removed(node.path, unmodifiableList(removedIndexes), unmodifiableList(removed));
		}
		if (!insertedIndexes.isEmpty()) {
			nodesListener.inserted(node.path, unmodifiableList(insertedIndexes));
		}
		List<Integer> changedIndexes = new ArrayList<>();
		for (int i = 0; i < retainedAfter.size(); i++) {
			//new instances, equal paths
			if (retainedBefore.get(i) != retainedAfter.get(i)) {
				changedIndexes.add(retainedIndexes.get(i));
			}
		}
		if (!changedIndexes.isEmpty()) {
			nodesListener.changed(node.path, unmodifiableList(changedIndexes));
		}
	}

	private void notifyExpansion() {
		if (!pendingCollapsed.isEmpty()) {
			List<NodePath<T>> collapsed = new ArrayList<>(pendingCollapsed);
			pendingCollapsed.clear();
			collapsed.forEach(expansion.collapsed::accept);
		}
		if (!pendingExpanded.isEmpty()) {
			List<NodePath<T>> expandedPaths = new ArrayList<>(pendingExpanded);
			pendingExpanded.clear();
			expandedPaths.sort(comparingInt(NodePath::depth));
			expandedPaths.forEach(expansion.expanded::accept);
		}
	}

	/**
	 * Starts loading the expanded nodes, whose ancestors are expanded, not yet loaded or being loaded.
	 * The root is only loaded on refresh.
	 */
	private void loadExpanded() {
		if (root.children == null) {
			return;
		}
		List<NodePath<T>> toLoad = new ArrayList<>();
		collectUnloaded(root, toLoad);
		for (NodePath<T> path : toLoad) {
			Node<T> node = nodeMap.get(path);
			if (node != null && node.children == null && !loader.tasks.containsKey(path) && expansion.expanded(path)) {
				loader.load(node, false);
			}
		}
	}

	/**
	 * Collects the paths of the nodes below the given one holding the given item, included or filtered, depth first.
	 */
	private void collectPaths(Node<T> node, T item, List<NodePath<T>> paths) {
		if (node.children != null) {
			for (Node<T> child : node.children) {
				if (child.path.item().equals(item)) {
					paths.add(child.path);
				}
				collectPaths(child, item, paths);
			}
		}
	}

	private void collectUnloaded(Node<T> node, List<NodePath<T>> toLoad) {
		for (Node<T> child : node.includedChildren) {
			if (expanded.contains(child.path)) {
				if (child.children == null) {
					if (!loader.tasks.containsKey(child.path)) {
						toLoad.add(child.path);
					}
				}
				else {
					collectUnloaded(child, toLoad);
				}
			}
		}
	}

	/**
	 * Sets the children of the given node, keeping the existing child nodes by item, their items replaced with the
	 * given instances, removing the ones no longer present.
	 */
	private void children(Node<T> node, List<T> items, long loadSequence) {
		touch(node);
		nodesChanged = true;
		Map<T, Node<T>> existing = new HashMap<>();
		if (node.children != null) {
			for (Node<T> child : node.children) {
				existing.put(child.path.item(), child);
			}
		}
		List<Node<T>> nodeChildren = new ArrayList<>(items.size());
		for (T item : items) {
			Node<T> child = existing.remove(item);
			if (child == null) {
				child = new Node<>(node.path.child(item), node, predicate.get() == null);
				nodeMap.put(child.path, child);
			}
			else if (child.path.item() != item) {
				replaceItem(child, item);
			}
			nodeChildren.add(child);
		}
		existing.values().forEach(this::unregister);
		node.children = nodeChildren;
		node.loadSequence = loadSequence;
	}

	/**
	 * Replaces the item of the given node, rebuilding its path and the paths of its loaded descendants.
	 */
	private void replaceItem(Node<T> node, T item) {
		touch(requireNonNull(node.parent));
		path(node, node.parent.path.child(item));
		rebuildPaths(node);
	}

	private void rebuildPaths(Node<T> node) {
		if (node.children != null) {
			touch(node);
			for (Node<T> child : node.children) {
				path(child, node.path.child(child.path.item()));
				rebuildPaths(child);
			}
		}
	}

	/**
	 * Sets the path of the given node, the node map keyed by it, so that the map holds the current items as well.
	 */
	private void path(Node<T> node, NodePath<T> path) {
		nodeMap.remove(node.path);
		node.path = path;
		nodeMap.put(path, node);
		//to be tested against the predicate again
		node.passes = null;
		nodesChanged = true;
	}

	/**
	 * Removes the given node and its loaded descendants from the node map, along with their expansion,
	 * cancelling loads in progress.
	 */
	private void unregister(Node<T> node) {
		expanded.removeIf(node.path::contains);
		loader.cancel(node.path);
		removeFromNodeMap(node);
	}

	private void removeFromNodeMap(Node<T> node) {
		nodeMap.remove(node.path);
		if (node.children != null) {
			node.children.forEach(this::removeFromNodeMap);
		}
	}

	/**
	 * Called off the UI thread when loading asynchronously, accessing the roots supplier, the children function and
	 * the leaves function alone, never the model.
	 * @param path the path of the node to load
	 * @param loadedBelow the paths of the loaded nodes below, which are loaded as well
	 * @param refresh true if the leaves function should be called for the node as well, in case it is not yet loaded,
	 * which is not loaded in case the leaves function reports it as a leaf
	 * @param interruptible true if loading should stop when the thread is interrupted
	 * @return the children, and the leaf status of the nodes reached which are not yet loaded
	 */
	private Fetched<T> fetch(NodePath<T> path, Set<NodePath<T>> loadedBelow, boolean refresh, boolean interruptible) {
		Fetched<T> result = new Fetched<>();
		if (refresh && leaves != null && !path.root() && !loadedBelow.contains(path)) {
			boolean pathLeaf = testLeaf(path);
			result.leaves.put(path, pathLeaf);
			if (pathLeaf) {
				return result;
			}
		}
		Deque<NodePath<T>> queue = new ArrayDeque<>();
		queue.add(path);
		while (!queue.isEmpty()) {
			interrupted(interruptible);
			NodePath<T> parent = queue.removeFirst();
			List<T> items = validate(parent.root() ? roots.get() : children.apply(parent));
			result.children.put(parent, items);
			//a loaded node is a leaf when it has no included children, whatever the leaves function reports
			List<NodePath<T>> unloaded = new ArrayList<>(items.size());
			for (T item : items) {
				NodePath<T> child = parent.child(item);
				if (loadedBelow.contains(child)) {
					queue.addLast(child);
				}
				else {
					unloaded.add(child);
				}
			}
			if (leaves != null && !unloaded.isEmpty()) {
				interrupted(interruptible);
				Set<NodePath<T>> leafPaths = testLeaves(unloaded);
				for (NodePath<T> child : unloaded) {
					result.leaves.put(child, leafPaths.contains(child));
				}
			}
		}

		return result;
	}

	private static void interrupted(boolean interruptible) {
		if (interruptible && Thread.currentThread().isInterrupted()) {
			throw new CancelException();
		}
	}

	private static <T> List<T> validate(Collection<T> items) {
		List<T> validated = new ArrayList<>(requireNonNull(items, "A tree node children may not be null"));
		Set<T> distinct = new HashSet<>(validated.size());
		for (T item : validated) {
			if (item == null) {
				throw new IllegalArgumentException("A tree node child may not be null");
			}
			if (!distinct.add(item)) {
				throw new IllegalArgumentException(SIBLINGS_MUST_BE_DISTINCT + item);
			}
		}

		return validated;
	}

	private void apply(Fetched<T> result, long loadSequence) {
		mutate(() -> {
			result.children.forEach((path, items) -> {
				Node<T> node = nodeMap.get(path);
				if (node != null && node.loadSequence < loadSequence) {
					children(node, items, loadSequence);
				}
			});
			result.leaves.forEach((path, leafStatus) -> {
				Node<T> node = nodeMap.get(path);
				if (node != null) {
					leaf(node, leafStatus, loadSequence);
				}
			});
			//the expanded paths naming nodes which turned out not to exist
			expanded.removeIf(path -> !mayExist(path));
		});
	}

	/**
	 * @return true if the node identified by the given path is in the model, or may be once the nodes above it
	 * have been loaded, false if its nearest ancestor in the model is loaded without it, or is not yet loaded and
	 * reported as a leaf by the leaves function, so never loaded
	 */
	private boolean mayExist(NodePath<T> path) {
		NodePath<T> ancestor = path;
		while (!nodeMap.containsKey(ancestor)) {
			ancestor = ancestor.parent();
			Node<T> node = nodeMap.get(ancestor);
			if (node != null) {
				return node.children == null && !node.leaf;
			}
		}

		return true;
	}

	private final class DefaultSelectionContext implements SelectionContext<T> {

		@Override
		public VisibleNodes<T> visible() {
			return visible;
		}

		@Override
		public TreeSelection<T> treeSelection(MultiSelection<NodePath<T>> selection) {
			return new DefaultTreeSelection<>(selection, visible, DefaultFilterTreeModel.this::expandHidden,
							DefaultFilterTreeModel.this::selectWhenVisible, DefaultFilterTreeModel.this::occurrences);
		}
	}

	private final class DefaultNodes implements Nodes<T> {

		@Override
		public List<NodePath<T>> children(NodePath<T> parent) {
			Node<T> node = nodeMap.get(requireNonNull(parent));

			return node == null ? emptyList() : node.includedPaths;
		}

		@Override
		public boolean contains(NodePath<T> path) {
			return nodeMap.containsKey(requireNonNull(path));
		}

		@Override
		public List<NodePath<T>> paths(T item) {
			requireNonNull(item);
			List<NodePath<T>> paths = new ArrayList<>();
			collectPaths(root, item, paths);

			return unmodifiableList(paths);
		}

		@Override
		public boolean included(NodePath<T> path) {
			Node<T> node = nodeMap.get(requireNonNull(path));

			return node != null && node.included;
		}

		@Override
		public boolean loaded(NodePath<T> path) {
			Node<T> node = nodeMap.get(requireNonNull(path));

			return node != null && node.children != null;
		}

		@Override
		public boolean leaf(NodePath<T> path) {
			Node<T> node = nodeMap.get(requireNonNull(path));

			return node == null || DefaultFilterTreeModel.this.leaf(node);
		}

		@Override
		public Loader<T> loader() {
			return loader;
		}

		@Override
		public void refresh() {
			refresh(root.path);
		}

		@Override
		public void refresh(NodePath<T> path) {
			Node<T> node = nodeMap.get(requireNonNull(path));
			if (node != null) {
				loader.load(node, true);
			}
		}

		@Override
		public void add(NodePath<T> parent, Collection<T> items) {
			Node<T> node = nodeMap.get(requireNonNull(parent));
			rejectNulls(items);
			if (node == null || node.children == null || items.isEmpty()) {
				return;
			}
			Set<T> siblings = new HashSet<>();
			for (Node<T> child : node.children) {
				siblings.add(child.path.item());
			}
			List<NodePath<T>> paths = new ArrayList<>(items.size());
			for (T item : items) {
				if (!siblings.add(item)) {
					throw new IllegalArgumentException(SIBLINGS_MUST_BE_DISTINCT + item);
				}
				paths.add(node.path.child(item));
			}
			Set<NodePath<T>> leafPaths = testLeaves(paths);
			long leafSequence = ++sequence;
			mutate(() -> {
				touch(node);
				nodesChanged = true;
				List<Node<T>> nodeChildren = new ArrayList<>(requireNonNull(node.children));
				for (T item : items) {
					Node<T> child = new Node<>(node.path.child(item), node, predicate.get() == null);
					child.leaf = leafPaths.contains(child.path);
					child.leafSequence = leafSequence;
					nodeMap.put(child.path, child);
					nodeChildren.add(child);
				}
				node.children = nodeChildren;
			});
		}

		@Override
		public void remove(Collection<NodePath<T>> paths) {
			for (NodePath<T> path : rejectNulls(paths)) {
				if (path.root()) {
					throw new IllegalArgumentException("The root can not be removed");
				}
			}
			mutate(() -> {
				for (NodePath<T> path : paths) {
					Node<T> node = nodeMap.get(path);
					if (node != null) {
						Node<T> parent = requireNonNull(node.parent);
						touch(parent);
						nodesChanged = true;
						List<Node<T>> nodeChildren = new ArrayList<>(requireNonNull(parent.children));
						nodeChildren.remove(node);
						parent.children = nodeChildren;
						unregister(node);
					}
				}
			});
		}

		@Override
		public void replace(NodePath<T> path, T item) {
			requireNonNull(item);
			if (requireNonNull(path).root()) {
				throw new IllegalArgumentException("The root item can not be replaced");
			}
			Node<T> node = nodeMap.get(path);
			if (node == null) {
				return;
			}
			Node<T> parent = requireNonNull(node.parent);
			NodePath<T> after = parent.path.child(item);
			if (item.equals(node.path.item())) {
				boolean leafStatus = node.children == null && testLeaf(after);
				long leafSequence = ++sequence;
				mutate(() -> {
					replaceItem(node, item);
					DefaultFilterTreeModel.this.leaf(node, leafStatus, leafSequence);
				});
				return;
			}
			for (Node<T> sibling : requireNonNull(parent.children)) {
				if (sibling != node && sibling.path.item().equals(item)) {
					throw new IllegalArgumentException(SIBLINGS_MUST_BE_DISTINCT + item);
				}
			}
			boolean leafStatus = node.children == null && testLeaf(after);
			long leafSequence = ++sequence;
			NodePath<T> before = node.path;
			UnaryOperator<NodePath<T>> mapping = new PathMapping<>(before, after);
			mutate(() -> {
				//the loads in progress at or below the path, the expanded ones are loaded again by their new paths
				loader.cancel(before);
				replaceItem(node, item);
				DefaultFilterTreeModel.this.leaf(node, leafStatus, leafSequence);
				List<NodePath<T>> remapped = new ArrayList<>();
				Iterator<NodePath<T>> iterator = expanded.iterator();
				while (iterator.hasNext()) {
					NodePath<T> expandedPath = iterator.next();
					if (before.contains(expandedPath)) {
						iterator.remove();
						remapped.add(mapping.apply(expandedPath));
					}
				}
				expanded.addAll(remapped);
			}, mapping);
		}

		@Override
		public IncludePredicate<NodePath<T>> predicate() {
			return predicate;
		}

		@Override
		public void filter() {
			DefaultFilterTreeModel.this.filter();
		}
	}

	private final class DefaultLoader implements Loader<T> {

		private final Map<NodePath<T>, LoadTask> tasks = new HashMap<>();
		private final State async = State.state(FilterModel.ASYNC.getOrThrow());
		private final State active = State.state();
		private final Event<Exception> exception = Event.event();

		private int synchronous = 0;

		@Override
		public State async() {
			return async;
		}

		@Override
		public ObservableState active() {
			return active.observable();
		}

		@Override
		public boolean active(NodePath<T> path) {
			return tasks.containsKey(requireNonNull(path));
		}

		@Override
		public Observer<Exception> exception() {
			return exception.observer();
		}

		/**
		 * @param node the node to load
		 * @param refresh true if the leaves function should be called for the node as well, which is otherwise not loaded
		 * in case it is not yet loaded and the leaves function has reported it as a leaf
		 */
		private void load(Node<T> node, boolean refresh) {
			if (!refresh && node.children == null && node.leaf) {
				//a leaf according to the leaves function is not loaded, the children function not called for it
				return;
			}
			cancel(node.path);
			LoadTask task = new LoadTask(node.path, loadedBelow(node), refresh, ++sequence);
			if (async.is() && Dispatcher.instance().bound()) {
				tasks.put(node.path, task);
				updateActive();
				task.worker = ProgressWorker.builder()
								.task(task)
								.execute();
			}
			else {
				loadSynchronously(task);
			}
		}

		private void loadSynchronously(LoadTask task) {
			tasks.put(task.path, task);
			synchronous++;
			updateActive();
			Fetched<T> result;
			try {
				result = fetch(task.path, task.loadedBelow, task.refresh, false);
			}
			catch (Exception e) {
				failed(task, e);
				return;
			}
			finished(task);
			try {
				apply(result, task.sequence);
			}
			finally {
				updateActive();
			}
		}

		/**
		 * Called when a load failed, or was cancelled by the children function. Collapses the node in case it is
		 * not loaded, so that it is not loaded again until it is expanded again, and resumes the loading of the
		 * expanded nodes, the load having cancelled the ones in progress below it.
		 * @param task the task
		 * @param exception the exception, null if cancelled
		 */
		private void failed(LoadTask task, @Nullable Exception exception) {
			finished(task);
			try {
				Node<T> node = nodeMap.get(task.path);
				if (node != null && node.children == null) {
					expansion.collapse(task.path);
				}
				loadExpanded();
				resolvePendingSelection();
			}
			finally {
				updateActive();
				if (exception != null) {
					onException(exception);
				}
			}
		}

		/**
		 * Removes the given task, the caller being the one to call {@link #updateActive()}, once what follows the load
		 * has been started, the loading of the nodes expanded below it for example, so that the loader is not reported
		 * as inactive in between.
		 */
		private void finished(LoadTask task) {
			if (tasks.get(task.path) == task) {
				tasks.remove(task.path);
			}
			if (task.worker == null) {
				synchronous--;
			}
		}

		/**
		 * Cancels the loads in progress at or below the given path, the caller being the one to call
		 * {@link #updateActive()}, once the load replacing them, if any, has been started.
		 * @param path the path
		 * @return true if a load at or above the given path is in progress, which may change the children of the node
		 */
		private boolean loading(NodePath<T> path) {
			for (NodePath<T> taskPath : tasks.keySet()) {
				if (taskPath.contains(path)) {
					return true;
				}
			}

			return false;
		}

		private void cancel(NodePath<T> path) {
			Iterator<LoadTask> iterator = tasks.values().iterator();
			while (iterator.hasNext()) {
				LoadTask task = iterator.next();
				if (task.worker != null && path.contains(task.path)) {
					iterator.remove();
					task.worker.cancel(true);
				}
			}
		}

		private void updateActive() {
			active.set(!tasks.isEmpty() || synchronous > 0);
		}

		private void onException(Exception exception) {
			this.exception.accept(exception);
			onLoadException.accept(exception);
		}

		private Set<NodePath<T>> loadedBelow(Node<T> node) {
			Set<NodePath<T>> loaded = new HashSet<>();
			Deque<Node<T>> queue = new ArrayDeque<>();
			queue.add(node);
			while (!queue.isEmpty()) {
				Node<T> next = queue.removeFirst();
				if (next.children != null) {
					loaded.add(next.path);
					queue.addAll(next.children);
				}
			}

			return loaded;
		}
	}

	private final class LoadTask implements ResultTaskHandler<Fetched<T>> {

		private final NodePath<T> path;
		private final Set<NodePath<T>> loadedBelow;
		private final boolean refresh;
		private final long sequence;

		private @Nullable ProgressWorker<?, ?> worker;

		private LoadTask(NodePath<T> path, Set<NodePath<T>> loadedBelow, boolean refresh, long sequence) {
			this.path = path;
			this.loadedBelow = loadedBelow;
			this.refresh = refresh;
			this.sequence = sequence;
		}

		@Override
		public Fetched<T> execute() throws Exception {
			return fetch(path, loadedBelow, refresh, true);
		}

		@Override
		public void onResult(Fetched<T> result) {
			if (loader.tasks.get(path) == this) {
				loader.finished(this);
				try {
					apply(result, sequence);
				}
				finally {
					loader.updateActive();
				}
			}
		}

		@Override
		public void onException(Exception exception) {
			if (loader.tasks.get(path) == this) {
				loader.failed(this, exception);
			}
		}

		@Override
		public void onCancelled() {
			//cancelled by the children function, a cancel() having removed the task already
			if (loader.tasks.get(path) == this) {
				loader.failed(this, null);
			}
		}
	}

	private final class DefaultExpansion implements Expansion<T> {

		private final Event<NodePath<T>> expanded = Event.event();
		private final Event<NodePath<T>> collapsed = Event.event();

		@Override
		public void expand(NodePath<T> path) {
			if (requireNonNull(path).root() || !mayExist(path)) {
				return;
			}
			List<NodePath<T>> toExpand = new ArrayList<>(path.depth());
			NodePath<T> ancestor = path;
			while (!ancestor.root()) {
				if (!DefaultFilterTreeModel.this.expanded.contains(ancestor)) {
					toExpand.add(0, ancestor);
				}
				ancestor = ancestor.parent();
			}
			if (toExpand.isEmpty()) {
				loadExpanded();
			}
			else {
				mutate(() -> {
					DefaultFilterTreeModel.this.expanded.addAll(toExpand);
					pendingExpanded.addAll(toExpand);
				});
			}
		}

		@Override
		public void collapse(NodePath<T> path) {
			if (DefaultFilterTreeModel.this.expanded.contains(requireNonNull(path))) {
				mutate(() -> {
					DefaultFilterTreeModel.this.expanded.remove(path);
					pendingCollapsed.add(path);
				});
			}
		}

		@Override
		public boolean expanded(NodePath<T> path) {
			NodePath<T> ancestor = requireNonNull(path);
			while (!ancestor.root()) {
				if (!DefaultFilterTreeModel.this.expanded.contains(ancestor)) {
					return false;
				}
				ancestor = ancestor.parent();
			}

			return true;
		}

		@Override
		public Collection<NodePath<T>> get() {
			List<NodePath<T>> paths = new ArrayList<>(DefaultFilterTreeModel.this.expanded.size());
			for (NodePath<T> path : DefaultFilterTreeModel.this.expanded) {
				//the current path, of a node in the model
				Node<T> node = nodeMap.get(path);
				paths.add(node == null ? path : node.path);
			}

			return unmodifiableList(paths);
		}

		@Override
		public void set(Collection<NodePath<T>> paths) {
			Set<NodePath<T>> toSet = new LinkedHashSet<>(rejectNulls(paths));
			toSet.removeIf(path -> path.root() || !mayExist(path));
			Set<NodePath<T>> current = DefaultFilterTreeModel.this.expanded;
			if (toSet.equals(current)) {
				loadExpanded();
				return;
			}
			mutate(() -> {
				for (NodePath<T> path : current) {
					if (!toSet.contains(path)) {
						pendingCollapsed.add(path);
					}
				}
				for (NodePath<T> path : toSet) {
					if (!current.contains(path)) {
						pendingExpanded.add(path);
					}
				}
				current.clear();
				current.addAll(toSet);
			});
		}

		@Override
		public Observer<NodePath<T>> expanded() {
			return expanded.observer();
		}

		@Override
		public Observer<NodePath<T>> collapsed() {
			return collapsed.observer();
		}
	}

	private final class DefaultVisibleNodes implements VisibleNodes<T> {

		private final Event<List<NodePath<T>>> changed = Event.event();

		private List<NodePath<T>> paths = emptyList();
		private Map<NodePath<T>, Integer> indexes = emptyMap();
		private boolean pendingChanges = false;

		@Override
		public List<NodePath<T>> get() {
			return paths;
		}

		@Override
		public int size() {
			return paths.size();
		}

		@Override
		public NodePath<T> get(int index) {
			return paths.get(index);
		}

		@Override
		public int indexOf(NodePath<T> path) {
			Integer index = indexes.get(requireNonNull(path));

			return index == null ? -1 : index;
		}

		@Override
		public Observer<List<NodePath<T>>> observer() {
			return changed.observer();
		}

		private void update() {
			List<NodePath<T>> updated = new ArrayList<>(paths.size());
			add(root, updated);
			if (!sameInstances(paths, updated)) {
				Map<NodePath<T>, Integer> updatedIndexes = new HashMap<>(updated.size());
				for (int i = 0; i < updated.size(); i++) {
					updatedIndexes.put(updated.get(i), i);
				}
				paths = unmodifiableList(updated);
				indexes = updatedIndexes;
				pendingChanges = true;
			}
		}

		private void add(Node<T> node, List<NodePath<T>> visiblePaths) {
			for (Node<T> child : node.includedChildren) {
				visiblePaths.add(child.path);
				if (child.children != null && expanded.contains(child.path)) {
					add(child, visiblePaths);
				}
			}
		}

		private void notifyPending() {
			if (pendingChanges) {
				pendingChanges = false;
				changed.accept(paths);
			}
		}

		private boolean sameInstances(List<NodePath<T>> first, List<NodePath<T>> second) {
			if (first.size() != second.size()) {
				return false;
			}
			for (int i = 0; i < first.size(); i++) {
				if (first.get(i) != second.get(i)) {
					return false;
				}
			}

			return true;
		}
	}

	private final class DefaultSort implements FilterTreeSort<T> {

		private final @Nullable Comparator<T> comparator;
		private final Event<Boolean> changed = Event.event();
		private final Value<SortOrder> order;

		private DefaultSort(@Nullable Comparator<T> comparator) {
			this.comparator = comparator;
			this.order = Value.builder()
							.nonNull(comparator == null ? SortOrder.UNSORTED : SortOrder.ASCENDING)
							.consumer(sortOrder -> changed.accept(sorted()))
							.build();
		}

		@Override
		public int compare(T item, T other) {
			if (comparator == null) {
				return 0;
			}
			switch (order.getOrThrow()) {
				case ASCENDING:
					return comparator.compare(item, other);
				case DESCENDING:
					return comparator.compare(other, item);
				default:
					return 0;
			}
		}

		@Override
		public void ascending() {
			order(SortOrder.ASCENDING);
		}

		@Override
		public void descending() {
			order(SortOrder.DESCENDING);
		}

		@Override
		public void clear() {
			order(SortOrder.UNSORTED);
		}

		@Override
		public SortOrder order() {
			return order.getOrThrow();
		}

		@Override
		public boolean sorted() {
			return comparator != null && order.getOrThrow() != SortOrder.UNSORTED;
		}

		@Override
		public Observer<Boolean> observer() {
			return changed.observer();
		}

		private void order(SortOrder sortOrder) {
			if (comparator != null) {
				order.set(sortOrder);
			}
		}
	}

	/**
	 * Maps paths at or below one path to the same paths below another.
	 */
	private static final class PathMapping<T> implements UnaryOperator<NodePath<T>> {

		private final NodePath<T> before;
		private final NodePath<T> after;

		private PathMapping(NodePath<T> before, NodePath<T> after) {
			this.before = before;
			this.after = after;
		}

		@Override
		public NodePath<T> apply(NodePath<T> path) {
			if (!before.contains(path)) {
				return path;
			}
			List<T> items = new ArrayList<>(after.items());
			List<T> pathItems = path.items();
			items.addAll(pathItems.subList(before.depth(), pathItems.size()));

			return nodePath(items);
		}
	}

	private static final class Node<T> {

		private final @Nullable Node<T> parent;

		private NodePath<T> path;
		//null until loaded, in the order the children function returned them
		private @Nullable List<Node<T>> children;
		private List<Node<T>> includedChildren = emptyList();
		//replaced rather than modified, so a list handed out stays as it was
		private List<NodePath<T>> includedPaths = emptyList();
		private boolean included;
		//whether the node passed the predicate when last tested, null when it has not been
		//tested since it was added or had its item replaced
		private @Nullable Boolean passes;
		//the load that last set the children
		private long loadSequence = 0;
		//true if the leaves function reported the node as a leaf when last called for it, while not yet loaded, false without one
		private boolean leaf = false;
		//the load, add or replace that last called the leaves function for the node
		private long leafSequence = 0;

		private Node(NodePath<T> path, @Nullable Node<T> parent, boolean included) {
			this.path = path;
			this.parent = parent;
			this.included = included;
		}

		private void included(List<Node<T>> includedChildren, List<NodePath<T>> includedPaths) {
			this.includedChildren = includedChildren;
			this.includedPaths = includedPaths;
		}
	}

	/**
	 * The result of a load.
	 */
	private static final class Fetched<T> {

		//the children by parent path, parents before children
		private final Map<NodePath<T>, List<T>> children = new LinkedHashMap<>();
		//the leaf status the leaves function reported, by path
		private final Map<NodePath<T>, Boolean> leaves = new HashMap<>();
	}

	/**
	 * The state of a group of children before a mutation.
	 */
	private static final class Snapshot<T> {

		private final List<NodePath<T>> paths;
		private final boolean leaf;

		private Snapshot(List<NodePath<T>> paths, boolean leaf) {
			this.paths = paths;
			this.leaf = leaf;
		}
	}

	private static final class DefaultIncludePredicate<T>
					extends AbstractValue<Predicate<NodePath<T>>> implements IncludePredicate<NodePath<T>> {

		private @Nullable Predicate<NodePath<T>> predicate;

		private DefaultIncludePredicate() {
			super(SET);
		}

		@Override
		protected @Nullable Predicate<NodePath<T>> getValue() {
			return predicate;
		}

		@Override
		protected void setValue(@Nullable Predicate<NodePath<T>> predicate) {
			this.predicate = predicate;
		}
	}

	private static final class RethrowExceptionHandler implements Consumer<Exception> {

		@Override
		public void accept(Exception exception) {
			throw Exceptions.runtime(exception);
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
		public Builder<T, ?> children(Function<NodePath<T>, Collection<T>> children) {
			return new DefaultBuilder<>(roots, requireNonNull(children));
		}
	}

	static final class DefaultBuilder<T> extends AbstractFilterTreeModelBuilder<T, DefaultBuilder<T>> {

		static final Builder.RootsStep ROOTS = new DefaultRootsStep();

		private DefaultBuilder(Supplier<Collection<T>> roots, Function<NodePath<T>, Collection<T>> children) {
			super(roots, children);
		}
	}
}
