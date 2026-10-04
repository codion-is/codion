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

import is.codion.common.model.component.tree.FilterTreeModel.VisibleNodes;
import is.codion.common.model.component.tree.NodePath;
import is.codion.common.model.selection.MultiSelection;
import is.codion.common.reactive.event.Event;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;

import org.jspecify.annotations.Nullable;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import static is.codion.common.model.selection.MultiSelection.multiSelection;
import static is.codion.swing.common.model.component.tree.DefaultSwingFilterTreeModel.createTreePath;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;

/**
 * <p>A {@link DefaultTreeSelectionModel} serving as the {@link IndexStore} of a common {@link MultiSelection}, whose
 * index and item facades it forwards to, the indexes being the rows of the visible nodes of the model.
 * <p>The selected paths are the canonical state, as in any {@link DefaultTreeSelectionModel}, the rows derived from
 * them via {@link VisibleNodes#indexOf(Object)}, not via the {@link javax.swing.tree.RowMapper} of a {@code JTree},
 * so the selection is correct whether a {@code JTree} is attached or not.
 */
final class DefaultTreeSelection<T> extends DefaultTreeSelectionModel implements FilterTreeSelection<T> {

	private final Event<?> changing = Event.event();
	private final Event<?> changed = Event.event();
	private final Event<?> adjusting = Event.event();
	private final State singleSelection = State.state(false);
	private final DefaultGrouping grouping = new DefaultGrouping();
	private final VisibleNodes<T> visible;
	private final MultiSelection<NodePath<T>> selection;

	//true while the model notifies its listeners of structural changes, during which a JTree removes
	//the selected paths below the nodes changed, re-indexing rather than changing the selection
	private boolean structural = false;
	//true while a change, for which changing() has been notified, is being made
	private boolean changingNotified = false;

	DefaultTreeSelection(VisibleNodes<T> visible) {
		this.visible = requireNonNull(visible);
		this.selection = multiSelection(visible, new TreeStore());
		singleSelection.addConsumer(singleSelectionMode ->
						setSelectionMode(singleSelectionMode ? SINGLE_TREE_SELECTION : DISCONTIGUOUS_TREE_SELECTION));
	}

	@Override
	public State singleSelection() {
		return singleSelection;
	}

	@Override
	public ObservableState multiple() {
		return selection.multiple();
	}

	@Override
	public ObservableState single() {
		return selection.single();
	}

	@Override
	public ObservableState present() {
		return selection.present();
	}

	@Override
	public Observer<?> changing() {
		return changing.observer();
	}

	@Override
	public Value<Integer> index() {
		return selection.index();
	}

	@Override
	public Indexes indexes() {
		return selection.indexes();
	}

	@Override
	public Value<NodePath<T>> item() {
		return selection.item();
	}

	@Override
	public Items<NodePath<T>> items() {
		return selection.items();
	}

	@Override
	public int count() {
		return selection.count();
	}

	@Override
	public void selectAll() {
		selection.selectAll();
	}

	@Override
	public Grouping grouping() {
		return grouping;
	}

	@Override
	public Observer<?> adjusting() {
		return adjusting.observer();
	}

	@Override
	public void clear() {
		clearSelection();
	}

	@Override
	public void setSelectionMode(int selectionMode) {
		if (getSelectionMode() != selectionMode) {
			super.clearSelection();
			super.setSelectionMode(selectionMode);
			singleSelection.set(selectionMode == SINGLE_TREE_SELECTION);
		}
	}

	@Override
	public void setSelectionPaths(TreePath @Nullable [] paths) {
		boolean notified = notifyChanging();
		try {
			super.setSelectionPaths(paths);
		}
		finally {
			changingNotified(notified);
		}
	}

	@Override
	public void addSelectionPaths(TreePath @Nullable [] paths) {
		boolean notified = notifyChanging();
		try {
			super.addSelectionPaths(paths);
		}
		finally {
			changingNotified(notified);
		}
	}

	@Override
	public void removeSelectionPaths(TreePath @Nullable [] paths) {
		boolean notified = notifyChanging();
		try {
			super.removeSelectionPaths(paths);
		}
		finally {
			changingNotified(notified);
		}
	}

	@Override
	public void clearSelection() {
		boolean notified = notifyChanging();
		try {
			super.clearSelection();
		}
		finally {
			changingNotified(notified);
		}
	}

	@Override
	protected void fireValueChanged(TreeSelectionEvent event) {
		//the change has been made, one made by a listener in response is a change of its own
		changingNotified = false;
		super.fireValueChanged(event);
		adjusting.run();
		if (!grouping.grouping) {
			changed.run();
		}
	}

	/**
	 * @param structural true while the model notifies its listeners of structural changes
	 * @return the previous value
	 */
	boolean structural(boolean structural) {
		boolean previous = this.structural;
		this.structural = structural;

		return previous;
	}

	/**
	 * Notifies {@link #changing()}, unless the change is structural or part of one already notified,
	 * {@link DefaultTreeSelectionModel} implementing some of its methods in terms of others overridden here,
	 * adding a path in single selection mode sets it for example.
	 * @return true if this call is the one to end the change, see {@link #changingNotified(boolean)}
	 */
	private boolean notifyChanging() {
		if (changingNotified) {
			return false;
		}
		if (!structural) {
			changing.run();
		}
		changingNotified = true;

		return true;
	}

	private void changingNotified(boolean notified) {
		if (notified) {
			changingNotified = false;
		}
	}

	private final class DefaultGrouping implements Grouping {

		private boolean grouping = false;

		@Override
		public void set(boolean grouping) {
			boolean ended = this.grouping && !grouping;
			this.grouping = grouping;
			if (ended) {
				//the facades are consulted regardless of whether the paths changed during the group,
				//the instances the selected rows refer to may have been replaced meanwhile
				changed.run();
			}
		}

		@Override
		public boolean is() {
			return grouping;
		}
	}

	/**
	 * The selected paths as an {@link IndexStore}, the rows of the visible nodes.
	 */
	private final class TreeStore implements IndexStore {

		@Override
		public Set<Integer> get() {
			Set<Integer> rows = new TreeSet<>();
			TreePath[] paths = getSelectionPaths();
			if (paths != null) {
				for (TreePath path : paths) {
					int row = visible.indexOf((NodePath<T>) path.getLastPathComponent());
					if (row >= 0) {
						rows.add(row);
					}
				}
			}

			return unmodifiableSet(rows);
		}

		@Override
		public void set(Collection<Integer> indexes) {
			set(indexes, true);
		}

		@Override
		public void restore(Collection<Integer> indexes) {
			set(indexes, false);
		}

		private void set(Collection<Integer> indexes, boolean notifyChanging) {
			TreeSet<Integer> rows = new TreeSet<>(indexes);
			if (singleSelection.is() && rows.size() > 1) {
				Integer keep = rows.last();
				rows.clear();
				rows.add(keep);
			}
			boolean rowsChanged = !rows.equals(get());
			if (!rowsChanged && rows.size() == getSelectionCount()) {
				return;
			}
			TreePath[] paths = new TreePath[rows.size()];
			int index = 0;
			for (Integer row : rows) {
				paths[index++] = createTreePath(visible.get(row));
			}
			//the selection does not change when the rows are unchanged, only the paths without a row being dropped,
			//the paths of nodes removed or filtered with no JTree attached to remove them
			if (notifyChanging && rowsChanged) {
				changing.run();
			}
			if (paths.length == 0) {
				DefaultTreeSelection.super.clearSelection();
			}
			else {
				DefaultTreeSelection.super.setSelectionPaths(paths);
			}
		}

		@Override
		public int size() {
			return get().size();
		}

		@Override
		public boolean contains(int index) {
			return index >= 0 && index < visible.size() && isPathSelected(createTreePath(visible.get(index)));
		}

		@Override
		public State singleSelection() {
			return singleSelection;
		}

		@Override
		public Grouping grouping() {
			return grouping;
		}

		@Override
		public Observer<?> changing() {
			return changing.observer();
		}

		@Override
		public Observer<?> changed() {
			return changed.observer();
		}

		@Override
		public Observer<?> adjusting() {
			return adjusting.observer();
		}
	}
}
