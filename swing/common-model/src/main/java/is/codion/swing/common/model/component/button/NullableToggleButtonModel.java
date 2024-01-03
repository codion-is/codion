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

import is.codion.common.value.Value;

import javax.swing.DefaultButtonModel;
import java.awt.event.ItemEvent;
import java.util.Objects;
import java.util.function.Consumer;

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

  private final Value<Boolean> buttonState = Value.value();

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
    setState(initialState);
  }

  /**
   * @return true if the underlying state is true
   */
  @Override
  public boolean isSelected() {
    return Objects.equals(buttonState.get(), Boolean.TRUE);
  }

  /**
   * Sets the underlying state to true or false
   * @param selected the new state
   */
  @Override
  public void setSelected(boolean selected) {
    setState(selected);
  }

  /**
   * Sets the underlying state
   * @param state the state
   */
  public void setState(Boolean state) {
    buttonState.set(state);
    fireItemStateChanged(new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED, this,
            state == null ? NULL : (state ? ItemEvent.SELECTED : ItemEvent.DESELECTED)));
    fireStateChanged();
  }

  /**
   * Returns the underlying value
   * @return the state
   */
  public Boolean getState() {
    return buttonState.get();
  }

  /**
   * Iterates between the states: null -&gt; false -&gt; true
   */
  public void nextState() {
    Boolean state = getState();
    if (state == null) {
      setState(false);
    }
    else if (!state) {
      setState(true);
    }
    else {
      setState(null);
    }
  }

  /**
   * Adds a listener notified each time the state changes.
   * @param listener the listener
   */
  public void addListener(Consumer<Boolean> listener) {
    buttonState.addDataListener(listener);
  }

  /**
   * Removes the given listener.
   * @param listener the listener to remove
   */
  public void removeListener(Consumer<Boolean> listener) {
    buttonState.removeDataListener(listener);
  }
}
