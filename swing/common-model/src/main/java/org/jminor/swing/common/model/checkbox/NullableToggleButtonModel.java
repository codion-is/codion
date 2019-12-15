/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.model.checkbox;

import org.jminor.common.value.Value;
import org.jminor.common.value.Values;

import javax.swing.JToggleButton;
import java.awt.event.ItemEvent;
import java.util.Objects;

/**
 * A ToggleButtonModel implementation, which allows null values.
 * The states are null -&gt; false -&gt; true.
 *
 * Heavily influenced by TristateCheckBox by Heinz M. Kabutz
 * http://www.javaspecialists.eu/archive/Issue145.html
 */
public final class NullableToggleButtonModel extends JToggleButton.ToggleButtonModel {

  private final Value<Boolean> buttonValue;

  /**
   * Instantiates a new {@link NullableToggleButtonModel} with a null initial value.
   */
  public NullableToggleButtonModel() {
    this(null);
  }

  /**
   * Instantiates a new {@link NullableToggleButtonModel} with the given initial value.
   * @param initialValue the initial value
   */
  public NullableToggleButtonModel(final Boolean initialValue) {
    this.buttonValue = Values.value(initialValue);
    bindEvents();
    displayState();
  }

  /**
   * @return true if the underlying value is true
   */
  @Override
  public boolean isSelected() {
    return Objects.equals(buttonValue.get(), Boolean.TRUE);
  }

  /**
   * Sets the underlying value to true or false
   * @param selected the value to set
   */
  @Override
  public void setSelected(final boolean selected) {
    set(selected);
  }

  /**
   * Sets the underlying value
   * @param value the value
   */
  public void set(final Boolean value) {
    buttonValue.set(value);
  }

  /**
   * Returns the underlying value
   * @return the value
   */
  public Boolean get() {
    return buttonValue.get();
  }

  /**
   * Iterates between the states: null -> false -> true
   */
  public void nextState() {
    final Boolean value = get();
    if (value == null) {
      set(false);
    }
    else if (!value) {
      set(true);
    }
    else {
      set(null);
    }
  }

  /**
   * Does nothing.
   * @param value the value
   */
  @Override
  public void setArmed(final boolean value) {/*N/A*/}

  /**
   * Does nothing.
   * @param value the value
   */
  @Override
  public void setPressed(final boolean value) {/*N/A*/}

  private void bindEvents() {
    buttonValue.addDataListener(value -> {
      displayState();
      fireStateChanged();
      fireItemStateChanged(new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED,this,
              value == null ? 3 : (value ?  ItemEvent.SELECTED : ItemEvent.DESELECTED)));
    });
  }

  private void displayState() {
    super.setArmed(buttonValue.get() == null);
    super.setPressed(buttonValue.get() == null);
  }
}
