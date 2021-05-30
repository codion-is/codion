/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.event.Event;
import is.codion.common.state.StateObserver;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
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
   * Creates a button based on this Control
   * @return a button based on this Control
   */
  JButton createButton();

  /**
   * Unsupported, the enabled state of Controls is based on their {@code enabledState}
   * @param enabled the enabled status
   * @throws UnsupportedOperationException always
   * @see Builder#enabledState(StateObserver)
   */
  @Override
  void setEnabled(boolean enabled);

  /**
   * A command interface, allowing Controls based on method references
   */
  interface Command {

    /**
     * Performs the work
     * @throws Exception in case of an exception
     */
    void perform() throws Exception;
  }

  /**
   * A command interface, allowing Controls based on {@link ActionEvent}s.
   */
  interface ActionCommand {

    /**
     * Performes the work.
     * @param actionEvent the action event
     * @throws Exception in case of an exception
     */
    void perform(ActionEvent actionEvent) throws Exception;
  }

  /**
   * Creates a control based on a {@link Control.Command}
   * @param command the {@link Control.Command} on which to base the control
   * @return a Control for calling the given {@link Control.Command}
   */
  static Control control(final Command command) {
    return builder(command).build();
  }

  /**
   * Creates a control based on a {@link Control.ActionCommand}
   * @param actionCommand the {@link Control.ActionCommand} on which to base the control
   * @return a Control for calling the given {@link Control.Command}
   */
  static Control actionControl(final ActionCommand actionCommand) {
    return actionControlBuilder(actionCommand).build();
  }

  /**
   * Creates a Control which triggers the given event on action performed
   * @param event the event
   * @return a control which triggers the given event
   */
  static Control eventControl(final Event<ActionEvent> event) {
    return eventControlBuilder(event).build();
  }

  /**
   * Creates a new Builder.
   * @param command the command to base the control on
   * @return a new Control.Builder
   */
  static Builder builder(final Command command) {
    return new ControlBuilder(command);
  }

  /**
   * Creates a new Builder.
   * @param actionCommand the action command to base the control on
   * @return a new Control.Builder
   */
  static Builder actionControlBuilder(final ActionCommand actionCommand) {
    return new ControlBuilder(actionCommand);
  }

  /**
   * Creates a Builder for a control which triggers the given event on action performed
   * @param event the event
   * @return a new Control.Builder
   */
  static Builder eventControlBuilder(final Event<ActionEvent> event) {
    requireNonNull(event, "event");
    return new ControlBuilder((ActionCommand) event::onEvent);
  }

  /**
   * A builder for Control
   */
  interface Builder {

    /**
     * @param name the name of the control
     * @return this Builder instance
     * @deprecated use {@link #caption(String)}
     */
    @Deprecated(since = "0.17.13", forRemoval = true)
    Builder name(String name);

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
     * @throws IllegalStateException in case no command has been set
     */
    Control build();
  }
}
