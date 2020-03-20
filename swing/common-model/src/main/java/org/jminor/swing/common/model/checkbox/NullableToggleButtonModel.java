/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.model.checkbox;

import org.jminor.common.event.EventDataListener;
import org.jminor.common.value.Value;
import org.jminor.common.value.Values;

import javax.swing.DefaultButtonModel;
import java.awt.event.ItemEvent;
import java.util.Objects;

/**
 * A ToggleButtonModel implementation, which allows the null state.
 * The states are null -&gt; false -&gt; true.
 *
 * Heavily influenced by TristateCheckBox by Heinz M. Kabutz
 * http://www.javaspecialists.eu/archive/Issue145.html
 */
public final class NullableToggleButtonModel extends DefaultButtonModel {

  /**
   * The item state NULL.
   * @see ItemEvent#SELECTED
   * @see ItemEvent#DESELECTED
   */
  public static final int NULL = 3;

  private final Value<Boolean> buttonState;

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
  public NullableToggleButtonModel(final Boolean initialState) {
    this.buttonState = Values.value(initialState);
    displayState(initialState);
    bindEvents();
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
  public void setSelected(final boolean selected) {
    setState(selected);
  }

  /**
   * Sets the underlying state
   * @param state the state
   */
  public void setState(final Boolean state) {
    buttonState.set(state);
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
    final Boolean state = getState();
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
   * Does nothing.
   * @param value the value
   */
  @Override
  public void setArmed(final boolean value) {/*Not implemented*/}

  /**
   * Does nothing.
   * @param value the value
   */
  @Override
  public void setPressed(final boolean value) {/*Not implemented*/}

  /**
   * Adds a listener notified each time the state changes.
   * @param listener the listener
   */
  public void addStateListener(final EventDataListener<Boolean> listener) {
    buttonState.addDataListener(listener);
  }

  /**
   * Removes the given listener.
   * @param listener the listener to remove
   */
  public void removeStateListener(final EventDataListener<Boolean> listener) {
    buttonState.removeDataListener(listener);
  }

  private void bindEvents() {
    buttonState.addDataListener(state -> {
      displayState(state);
      fireStateChanged();
      fireItemStateChanged(new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED,this,
              state == null ? NULL : (state ?  ItemEvent.SELECTED : ItemEvent.DESELECTED)));
    });
  }

  private void displayState(final Boolean state) {
    super.setArmed(state == null);
    super.setPressed(state == null);
  }
}
