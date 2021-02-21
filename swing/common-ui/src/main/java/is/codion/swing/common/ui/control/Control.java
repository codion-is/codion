/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.event.Event;
import is.codion.common.state.StateObserver;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;

import static java.util.Objects.requireNonNull;

/**
 * A beefed up Action.
 */
public interface Control extends Action {

  /**
   * @param description the description string
   * @return this control instance
   */
  Control setDescription(String description);

  /**
   * @return the description
   */
  String getDescription();

  /**
   * @return the name
   */
  String getName();

  /**
   * @param name the name of this Control instance
   * @return this Control instance
   */
  Control setName(String name);

  /**
   * @return the state which controls whether this Control instance is enabled
   */
  StateObserver getEnabledObserver();

  /**
   * @param mnemonic the mnemonic to associate with this Control instance
   * @return this Control instance
   */
  Control setMnemonic(int mnemonic);

  /**
   * @return the mnemonic, 0 if none is specified
   */
  int getMnemonic();

  /**
   * @param keyStroke the KeyStroke to associate with this Control
   * @return this Control instance
   */
  Control setKeyStroke(KeyStroke keyStroke);

  /**
   * @return the KeyStroke associated with this Control, if any
   */
  KeyStroke getKeyStroke();

  /**
   * @param icon the icon to associate with this Control
   * @return this Control instance
   */
  Control setIcon(Icon icon);

  /**
   * @return the icon
   */
  Icon getIcon();

  /**
   * A simple command interface, allowing for Controls based on method references
   */
  interface Command {

    /**
     * Performs the work
     * @throws Exception in case of an exception
     */
    void perform() throws Exception;
  }

  /**
   * Creates a control based on a {@link Control.Command}
   * @param command the {@link Control.Command} on which to base this control
   * @return a Control for calling the given {@link Control.Command}
   */
  static Control control(final Command command) {
    return builder().command(command).build();
  }

  /**
   * Creates a Control which triggers the given event on action performed
   * @param event the event
   * @return a control which triggers the given event
   */
  static Control control(final Event<ActionEvent> event) {
    requireNonNull(event, "event");
    return control(event::onEvent);
  }

  /**
   * @return a new Control.Builder
   */
  static Builder builder() {
    return new ControlBuilder();
  }

  /**
   * A builder for Control
   */
  interface Builder {

    /**
     * @param command the {@link Control.Command} on which to base this control
     * @return this Builder instance
     */
    Builder command(Command command);

    /**
     * @param name the name of the control
     * @return this Builder instance
     */
    Builder name(String name);

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
     * @return a new Control instance
     */
    Control build();
  }
}
