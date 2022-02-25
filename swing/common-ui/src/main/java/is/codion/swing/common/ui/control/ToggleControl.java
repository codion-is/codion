/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
   * @return a check-box menu item based on this control
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
   * @return a check-box
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
  static ToggleControl toggleControl(Value<Boolean> value) {
    return builder(value).build();
  }

  /**
   * Creates a new ToggleControl based on the given state
   * @param state the state
   * @return a new ToggleControl
   */
  static ToggleControl toggleControl(State state) {
    return builder(state).build();
  }

  /**
   * @param value the value to toggle
   * @return a new ToggleControl.Builder
   */
  static Builder builder(Value<Boolean> value) {
    return new ToggleControlBuilder(value);
  }

  /**
   * @param state the state to toggle
   * @return a new ToggleControl.Builder
   */
  static Builder builder(State state) {
    return new ToggleControlBuilder(state);
  }

  /**
   * A builder for ToggleControl
   */
  interface Builder extends Control.Builder {

    /**
     * @param caption the caption of the control
     * @return this Builder instance
     */
    Builder caption(String caption);

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
     * @param smallIcon the small control icon
     * @return this Builder instance
     */
    Builder smallIcon(Icon smallIcon);

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
