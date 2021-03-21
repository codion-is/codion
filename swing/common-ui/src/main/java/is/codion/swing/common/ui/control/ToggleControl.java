/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;

import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;

/**
 * A Control for toggling a boolean value.
 */
public interface ToggleControl extends Control {

  /**
   * @return the value being toggled by this toggle control
   */
  Value<Boolean> getValue();

  /**
   * @return a check box menu item based on this control
   * @throws IllegalArgumentException in case this toggle control value is nullable
   */
  JCheckBoxMenuItem createCheckBoxMenuItem();

  /**
   * @return a radio button menu item based on this control
   * @throws IllegalArgumentException in case this toggle control value is nullable
   */
  JRadioButtonMenuItem createRadioButtonMenuItem();

    /**
   * Creates a ButtonModel based on this {@link ToggleControl}.
   * If the underlying value is nullable then a NullableButtonModel is returned.
   * @return a button model
   */
  ButtonModel createButtonModel();

  /**
   * Creates a JCheckBox based on this toggle control
   * @return a check box
   */
  JCheckBox createCheckBox();

  /**
   * Creates a JToggleButton based on this toggle control
   * @return a toggle button
   */
  JToggleButton createToggleButton();

  /**
   * Creates a new ToggleControl based on the given value
   * @param value the value
   * @return a new ToggleControl
   */
  static ToggleControl toggleControl(final Value<Boolean> value) {
    return builder().value(value).build();
  }

  /**
   * Creates a new ToggleControl based on the given state
   * @param state the state
   * @return a new ToggleControl
   */
  static ToggleControl toggleControl(final State state) {
    return builder().state(state).build();
  }

  /**
   * @return a new ToggleControl.Builder
   */
  static Builder builder() {
    return new ToggleControlBuilder();
  }

  /**
   * A builder for ToggleControl
   */
  interface Builder extends Control.Builder {

    /**
     * @param name the name of the control
     * @return this Builder instance
     */
    Builder name(String name);

    /**
     * @param state the state
     * @return this Builder instance
     */
    Builder state(State state);

    /**
     * @param value the value
     * @return this Builder instance
     */
    Builder value(Value<Boolean> value);

    /**
     * @param enabledState the state which controls the enabled state of the control
     * @return this Builder instance
     */
    Builder enabledState(StateObserver enabledState);

    /**
     * @param mnemonic the control mnemonic
     * @return this Builder instance
     */
    Builder mnemonic(char mnemonic);

    /**
     * @param icon the control icon
     * @return this Builder instance
     */
    Builder icon(Icon icon);

    /**
     * @param description a string describing the control
     * @return this Builder instance
     */
    Builder description(String description);

    /**
     * @param keyStroke the keystroke to associate with the control
     * @return this Builder instance
     */
    Builder keyStroke(KeyStroke keyStroke);

    /**
     * @return a new ToggleControl
     */
    ToggleControl build();
  }
}
