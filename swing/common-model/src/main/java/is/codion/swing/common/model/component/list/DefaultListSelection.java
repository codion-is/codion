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
 * Copyright (c) 2013 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.component.list;

import is.codion.common.model.filter.FilterModel.IncludedItems;
import is.codion.common.model.selection.MultiSelection;
import is.codion.common.reactive.event.Event;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;

import javax.swing.DefaultListSelectionModel;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import static is.codion.common.model.selection.MultiSelection.multiSelection;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;

/**
 * A {@link DefaultListSelectionModel} serving as the {@link IndexStore} of a common {@link MultiSelection}, whose index
 * and item facades it forwards to. The keyboard actions moving the lead without changing the selection are enabled
 * only for a {@link DefaultListSelectionModel} (BasicTableUI and BasicListUI), hence the extension.
 */
final class DefaultListSelection<R> extends DefaultListSelectionModel implements FilterListSelection<R> {

	private final Event<?> changing = Event.event();
	private final Event<?> changed = Event.event();
	private final State singleSelection = State.state(false);
	private final MultiSelection<R> selection;

	DefaultListSelection(IncludedItems<R> items) {
		this.selection = multiSelection(requireNonNull(items), new ListStore());
		singleSelection.addConsumer(singleSelectionMode ->
						setSelectionMode(singleSelectionMode ? SINGLE_SELECTION : MULTIPLE_INTERVAL_SELECTION));
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
	public ObservableState empty() {
		return selection.empty();
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
	public Value<R> item() {
		return selection.item();
	}

	@Override
	public Items<R> items() {
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
	public void adjusting(boolean adjusting) {
		setValueIsAdjusting(adjusting);
	}

	@Override
	public boolean adjusting() {
		return getValueIsAdjusting();
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
			singleSelection.set(selectionMode == SINGLE_SELECTION);
		}
	}

	@Override
	public void addSelectionInterval(int fromIndex, int toIndex) {
		changing.run();
		super.addSelectionInterval(fromIndex, toIndex);
	}

	@Override
	public void setSelectionInterval(int fromIndex, int toIndex) {
		changing.run();
		super.setSelectionInterval(fromIndex, toIndex);
	}

	@Override
	public void removeSelectionInterval(int fromIndex, int toIndex) {
		changing.run();
		super.removeSelectionInterval(fromIndex, toIndex);
	}

	@Override
	public void setLeadSelectionIndex(int leadIndex) {
		//shift+click and shift+arrow extension go through here (JTable.changeSelection with extend=true),
		//changing the anchor-to-lead selection range, so it must fire the changing() veto point too
		changing.run();
		super.setLeadSelectionIndex(leadIndex);
	}

	@Override
	public void clearSelection() {
		changing.run();
		super.clearSelection();
	}

	@Override
	public void setValueIsAdjusting(boolean isAdjusting) {
		boolean wasAdjusting = getValueIsAdjusting();
		super.setValueIsAdjusting(isAdjusting);
		if (wasAdjusting && !isAdjusting) {
			//DefaultListSelectionModel fires at the end of an adjustment only if an index changed during it, the facades
			//are consulted regardless, the instances the selected indexes refer to may have been replaced meanwhile
			changed.run();
		}
	}

	@Override
	protected void fireValueChanged(int firstIndex, int lastIndex, boolean isAdjusting) {
		super.fireValueChanged(firstIndex, lastIndex, isAdjusting);
		if (!isAdjusting) {
			changed.run();
		}
	}

	/**
	 * The {@link DefaultListSelectionModel} as a {@link IndexStore}. Structural changes made by a JTable or JList,
	 * insertIndexInterval() and removeIndexInterval(), reach {@link #changed()} via fireValueChanged() without
	 * passing {@link #changing()}, they re-index the selection rather than change it.
	 */
	private final class ListStore implements IndexStore {

		@Override
		public Set<Integer> get() {
			Set<Integer> selected = new TreeSet<>();
			if (!isSelectionEmpty()) {
				for (int index = getMinSelectionIndex(); index <= getMaxSelectionIndex(); index++) {
					if (isSelectedIndex(index)) {
						selected.add(index);
					}
				}
			}

			return unmodifiableSet(selected);
		}

		@Override
		public void set(Collection<Integer> indexes) {
			Set<Integer> current = get();
			Set<Integer> toRemove = new TreeSet<>(current);
			toRemove.removeAll(indexes);
			Set<Integer> toAdd = new TreeSet<>(indexes);
			toAdd.removeAll(current);
			if (toRemove.isEmpty() && toAdd.isEmpty()) {
				return;
			}
			changing.run();
			//save/restore so a caller already grouping (adjusting == true) is not terminated early
			boolean wasAdjusting = getValueIsAdjusting();
			setValueIsAdjusting(true);
			for (Integer index : toRemove) {
				DefaultListSelection.super.removeSelectionInterval(index, index);
			}
			for (Integer index : toAdd) {
				DefaultListSelection.super.addSelectionInterval(index, index);
			}
			setValueIsAdjusting(wasAdjusting);
		}

		@Override
		public boolean contains(int index) {
			return isSelectedIndex(index);
		}

		@Override
		public State singleSelection() {
			return singleSelection;
		}

		@Override
		public boolean adjusting() {
			return getValueIsAdjusting();
		}

		@Override
		public void adjusting(boolean adjusting) {
			setValueIsAdjusting(adjusting);
		}

		@Override
		public Observer<?> changing() {
			return changing.observer();
		}

		@Override
		public Observer<?> changed() {
			return changed.observer();
		}
	}
}
