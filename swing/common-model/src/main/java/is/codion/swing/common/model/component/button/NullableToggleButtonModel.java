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
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.component.button;

import is.codion.common.event.Event;
import is.codion.common.observer.Observable;
import is.codion.common.observer.Observer;

import javax.swing.DefaultButtonModel;
import java.awt.event.ItemEvent;
import java.util.Objects;

/**
 * A ToggleButtonModel implementation, which allows the null state.
 * The states are null -&gt; false -&gt; true.
 * Heavily influenced by TristateCheckBox by Heinz M. Kabutz
 * <a href="http://www.javaspecialists.eu/archive/Issue145.html">http://www.javaspecialists.eu/archive/Issue145.html</a>
 * Included with permission.
 * @author Heinz M. Kabutz
 * @author Björn Darri Sigurðsson
 */
public final class NullableToggleButtonModel extends DefaultButtonModel {

	/**
	 * The item state NULL.
	 * @see ItemEvent#SELECTED
	 * @see ItemEvent#DESELECTED
	 */
	public static final int NULL = 3;

	private final ToggleState toggleState = new ToggleState();

	/**
	 * Instantiates a new {@link NullableToggleButtonModel} with a null initial state.
	 */
	public NullableToggleButtonModel() {
		this(null);
	}

	/**
	 * Instantiates a new {@link NullableToggleButtonModel} with the given initial state.
	 * @param initialState the initial state
	 */
	public NullableToggleButtonModel(Boolean initialState) {
		toggleState.set(initialState);
	}

	/**
	 * @return true if the underlying state is true
	 */
	@Override
	public boolean isSelected() {
		return Objects.equals(toggleState.get(), Boolean.TRUE);
	}

	/**
	 * Sets the underlying state to true or false
	 * @param selected the new state
	 */
	@Override
	public void setSelected(boolean selected) {
		toggleState.set(selected);
	}

	/**
	 * @return an {@link Observable} controlling the toggle state
	 */
	public ToggleState toggleState() {
		return toggleState;
	}

	public final class ToggleState implements Observable<Boolean> {

		private final Event<Boolean> event = Event.event();

		private Boolean state = null;

		@Override
		public Boolean get() {
			return state;
		}

		@Override
		public void set(Boolean state) {
			this.state = state;
			fireItemStateChanged(new ItemEvent(NullableToggleButtonModel.this, ItemEvent.ITEM_STATE_CHANGED, this,
							state == null ? NULL : (state ? ItemEvent.SELECTED : ItemEvent.DESELECTED)));
			fireStateChanged();
			event.accept(state);
		}

		/**
		 * Iterates between the states: null -&gt; false -&gt; true
		 */
		public void next() {
			if (state == null) {
				set(false);
			}
			else if (!state) {
				set(true);
			}
			else {
				set(null);
			}
		}

		@Override
		public Observer<Boolean> observer() {
			return event.observer();
		}
	}
}
