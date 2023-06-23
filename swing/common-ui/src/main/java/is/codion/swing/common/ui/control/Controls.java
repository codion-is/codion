/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.state.StateObserver;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.util.List;

/**
 * A collection of controls and separators, note that these can be nested controls.
 */
public interface Controls extends Control {

  /**
   * @return an unmodifiable view of the actions in this set
   */
  List<Action> actions();

  /**
   * Adds the given action to this Controls instance,
   * adding a null action has the same effect as addSeparator()
   * @param action the action to add
   * @return this Controls instance
   */
  Controls add(Action action);

  /**
   * Adds the given action to this Controls instance at the specified index,
   * adding a null action has the same effect as addSeparator()
   * @param index the index
   * @param action the action to add at the specified index
   * @return this Controls instance
   */
  Controls addAt(int index, Action action);

  /**
   * @param action the action to remove
   * @return this Controls instance
   */
  Controls remove(Action action);

  /**
   * Removes all actions from this controls instance
   * @return this Controls instance
   */
  Controls removeAll();

  /**
   * @return the number of controls in this controls instance
   */
  int size();

  /**
   * @return true if this controls instance contains no controls (or separators)
   */
  boolean isEmpty();

  /**
   * @return true if this controls instance contains controls (or separators)
   */
  boolean isNotEmpty();

  /**
   * @param index the index
   * @return the action at the given index
   */
  Action get(int index);

  /**
   * @param controls the controls to add
   * @return this Control instance
   */
  Controls add(Controls controls);

  /**
   * @param index the index
   * @param controls the controls to add at the specified index
   * @return this Controls instance
   */
  Controls addAt(int index, Controls controls);

  /**
   * Adds a separator to the end of this controls instance
   * @return this Controls instance
   */
  Controls addSeparator();

  /**
   * Adds a separator at the given index
   * @param index the index
   * @return this Controls instance
   */
  Controls addSeparatorAt(int index);

  /**
   * Adds all actions found in {@code controls} to this controls instance
   * @param controls the source list
   * @return this Controls instance
   */
  Controls addAll(Controls controls);

  /**
   * Constructs a new Controls instance.
   * @return a new Controls instance.
   */
  static Controls controls() {
    return builder().build();
  }

  /**
   * Constructs a new Controls instance.
   * @param actions the actions
   * @return a new Controls instance.
   */
  static Controls controls(Action... actions) {
    return builder().actions(actions).build();
  }

  /**
   * Constructs a new Controls instance.
   * @param controls the controls
   * @return a new Controls instance.
   */
  static Controls controls(Control... controls) {
    return builder().controls(controls).build();
  }

  /**
   * @return a new Controls.Builder instance
   */
  static Builder builder() {
    return new ControlsBuilder();
  }

  /**
   * A builder for Controls
   * @see Controls#builder(Command)
   * @see Controls#actionControlBuilder(ActionCommand)
   */
  interface Builder extends Control.Builder {

    /**
     * @param name the name for this controls instance
     * @return this Builder instance
     */
    Builder name(String name);

    /**
     * @param description a description for this controls instance
     * @return this Builder instance
     */
    Builder description(String description);

    /**
     * @param mnenomic the mnemonic to assign to this controls instance
     * @return this Builder instance
     */
    Builder mnemonic(char mnenomic);

    /**
     * @param keyStroke the keystroke to associate with this controls instance
     * @return this Builder instance
     */
    Builder keyStroke(KeyStroke keyStroke);

    /**
     * @param enabledState the state observer dictating the enabled state of this controls instance
     * @return this Builder instance
     */
    Builder enabledState(StateObserver enabledState);

    /**
     * @param smallIcon the icon
     * @return this Builder instance
     */
    Builder smallIcon(Icon smallIcon);

    /**
     * @param control the control to add to this controls instance
     * @return this Builder instance
     */
    Builder control(Control control);

    /**
     * @param controlBuilder the control builder to add to this controls instance
     * @return this Builder instance
     */
    Builder control(Control.Builder controlBuilder);

    /**
     * @param controls the controls to add
     * @return this Builder instance
     */
    Builder controls(Control... controls);

    /**
     * @param controlBuilders the control builder to add
     * @return this Builder instance
     */
    Builder controls(Control.Builder... controlBuilders);

    /**
     * @param action the Action to add to this controls instance
     * @return this Builder instance
     */
    Builder action(Action action);

    /**
     * @param actions the Actions to add to this controls instance
     * @return this Builder instance
     */
    Builder actions(Action... actions);

    /**
     * Adds a separator to the Controls
     * @return this Builder instance
     */
    Builder separator();

    /**
     * Builds the Controls
     * @return a new Controls instance
     */
    Controls build();
  }
}
