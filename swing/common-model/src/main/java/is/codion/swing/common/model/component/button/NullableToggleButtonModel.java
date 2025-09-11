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
 * Copyright (c) Heinz M. Kabutz.
 */
package is.codion.swing.common.model.component.button;

import is.codion.common.value.Value;

import org.jspecify.annotations.Nullable;

import javax.swing.DefaultButtonModel;
import java.awt.event.ItemEvent;

/**
 * A ToggleButtonModel implementation, which allows the null state.
 * The states are null -&gt; false -&gt; true.
 * Heavily influenced by TristateCheckBox by Heinz M. Kabutz
 * <a href="http://www.javaspecialists.eu/archive/Issue145.html">http://www.javaspecialists.eu/archive/Issue145.html</a>
 * Included with express permission from the author, 2019.
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

	private final Value<Boolean> value;

	private NullableToggleButtonModel(@Nullable Boolean initialState) {
		value = Value.builder()
						.nullable(initialState)
						.consumer(this::onStateChanged)
						.build();
	}

	/**
	 * @return true if the underlying state is true
	 */
	@Override
	public boolean isSelected() {
		return value.is(true);
	}

	/**
	 * Sets the underlying state to true or false
	 * @param selected the new state
	 */
	@Override
	public void setSelected(boolean selected) {
		value.set(selected);
	}

	/**
	 * @return the toggle state
	 */
	public @Nullable Boolean get() {
		return value.get();
	}

	/**
	 * @param state the toggle state
	 */
	public void set(@Nullable Boolean state) {
		this.value.set(state);
	}

	/**
	 * Iterates between the states: null -&gt; false -&gt; true
	 */
	public void next() {
		Boolean state = value.get();
		if (state == null) {
			value.set(false);
		}
		else if (!state) {
			value.set(true);
		}
		else {
			value.set(null);
		}
	}

	/**
	 * Instantiates a new {@link NullableToggleButtonModel} with a null initial state.
	 */
	public static NullableToggleButtonModel nullableToggleButtonModel() {
		return new NullableToggleButtonModel(null);
	}

	/**
	 * Instantiates a new {@link NullableToggleButtonModel} with the given initial state.
	 * @param initialState the initial state
	 */
	public static NullableToggleButtonModel nullableToggleButtonModel(@Nullable Boolean initialState) {
		return new NullableToggleButtonModel(initialState);
	}

	private void onStateChanged(Boolean state) {
		fireItemStateChanged(new ItemEvent(NullableToggleButtonModel.this, ItemEvent.ITEM_STATE_CHANGED, this,
						state == null ? NULL : (state ? ItemEvent.SELECTED : ItemEvent.DESELECTED)));
		fireStateChanged();
	}
}
