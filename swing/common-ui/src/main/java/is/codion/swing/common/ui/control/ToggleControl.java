/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;

import javax.swing.Icon;
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
   * Creates a new ToggleControl based on the given value
   * @param value the value
   * @return a new ToggleControl
   */
  static ToggleControl toggleControl(final Value<Boolean> value) {
    return toggleControlBuilder().value(value).build();
  }

  /**
   * Creates a new ToggleControl based on the given state
   * @param state the state
   * @return a new ToggleControl
   */
  static ToggleControl toggleControl(final State state) {
    return toggleControlBuilder().state(state).build();
  }

  /**
   * @return a new ToggleControl.Builder
   */
  static Builder toggleControlBuilder() {
    return new ToggleControlBuilder();
  }

  /**
   * A builder for ToggleControl
   */
  interface Builder {

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
