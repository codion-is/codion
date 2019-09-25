/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.jminor.common.Event;
import org.jminor.common.EventObserver;
import org.jminor.common.State;
import org.jminor.common.StateObserver;
import org.jminor.common.Value;
import org.jminor.common.Values;
import org.jminor.common.model.CancelException;
import org.jminor.swing.common.model.checkbox.TristateButtonModel;
import org.jminor.swing.common.ui.UiValues;

import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.util.Objects;

/**
 * A factory class for Control objects.
 */
public final class Controls {

  private Controls() {}

  /**
   * Creates a control based on a {@link Control.Command}
   * @param command the {@link Control.Command} on which to base this control
   * @return a Control for calling the given {@link Control.Command}
   */
  public static Control control(final Control.Command command) {
    return control(command, (String) null);
  }

  /**
   * Creates a control based on a {@link Control.Command}
   * @param command the {@link Control.Command} on which to base this control
   * @param icon the icon
   * @return a Control for calling the given {@link Control.Command}
   */
  public static Control control(final Control.Command command, final Icon icon) {
    return control(command, null, null, null, -1, null, icon);
  }

  /**
   * Creates a control based on a {@link Control.Command}
   * @param command the {@link Control.Command} on which to base this control
   * @param icon the icon
   * @param enabledState the state which controls the enabled state of the control
   * @return a Control for calling the given {@link Control.Command}
   */
  public static Control control(final Control.Command command, final Icon icon, final StateObserver enabledState) {
    return control(command, null, enabledState, null, -1, null, icon);
  }

  /**
   * Creates a control based on a {@link Control.Command}
   * @param command the {@link Control.Command} on which to base this control
   * @param name the name of the control
   * @return a Control for calling the given {@link Control.Command}
   */
  public static Control control(final Control.Command command, final String name) {
    return control(command, name, null);
  }

  /**
   * Creates a control based on a {@link Control.Command}
   * @param command the {@link Control.Command} on which to base this control
   * @param name the name of the control
   * @param enabledState the state which controls the enabled state of the control
   * @return a Control for calling the given {@link Control.Command}
   */
  public static Control control(final Control.Command command, final String name, final StateObserver enabledState) {
    return new CommandControl(command, name, enabledState);
  }

  /**
   * Creates a control based on a {@link Control.Command}
   * @param command the {@link Control.Command} on which to base this control
   * @param name the name of the control
   * @param enabledState the state which controls the enabled state of the control
   * @param description a string describing the control
   * @return a Control for calling the given {@link Control.Command}
   */
  public static Control control(final Control.Command command, final String name, final StateObserver enabledState,
                                final String description) {
    return control(command, name, enabledState).setDescription(description);
  }

  /**
   * Creates a control based on a {@link Control.Command}
   * @param command the {@link Control.Command} on which to base this control
   * @param name the name of the control
   * @param enabledState the state which controls the enabled state of the control
   * @param description a string describing the control
   * @param mnemonic the control mnemonic
   * @return a Control for calling the given {@link Control.Command}
   */
  public static Control control(final Control.Command command, final String name, final StateObserver enabledState,
                                final String description, final int mnemonic) {
    return control(command, name, enabledState, description).setMnemonic(mnemonic);
  }

  /**
   * Creates a control based on a {@link Control.Command}
   * @param command the {@link Control.Command} on which to base this control
   * @param name the name of the control
   * @param enabledState the state which controls the enabled state of the control
   * @param description a string describing the control
   * @param mnemonic the control mnemonic
   * @param keyStroke the keystroke to associate with the control
   * @return a Control for calling the given {@link Control.Command}
   */
  public static Control control(final Control.Command command, final String name, final StateObserver enabledState,
                                final String description, final int mnemonic, final KeyStroke keyStroke) {
    return control(command, name, enabledState, description, mnemonic).setKeyStroke(keyStroke);
  }

  /**
   * Creates a control based on a {@link Control.Command}
   * @param command the {@link Control.Command} on which to base this control
   * @param name the name of the control
   * @param enabledState the state which controls the enabled state of the control
   * @param description a string describing the control
   * @param mnemonic the control mnemonic
   * @param keyStroke the keystroke to associate with the control
   * @param icon the control icon
   * @return a Control for calling the given {@link Control.Command}
   */
  public static Control control(final Control.Command command, final String name, final StateObserver enabledState,
                                final String description, final int mnemonic, final KeyStroke keyStroke, final Icon icon) {
    return control(command, name, enabledState, description, mnemonic, keyStroke).setIcon(icon);
  }

  /**
   * Creates a toggle control based on the boolean property {@code beanPropertyName} in the owner object
   * @param owner the owner object
   * @param beanPropertyName the name of the boolean bean property, must have a public setter and getter
   * @param caption the control caption
   * @return a toggle control
   */
  public static ToggleControl toggleControl(final Object owner, final String beanPropertyName, final String caption) {
    return toggleControl(owner, beanPropertyName, caption, null);
  }

  /**
   * Creates a toggle control based on the boolean property {@code beanPropertyName} in the owner object
   * @param owner the owner object
   * @param beanPropertyName the name of the boolean bean property, must have a public setter and getter
   * @param caption the control caption
   * @param changeEvent an event fired each time the property value changes in the underlying object
   * @return a toggle control
   */
  public static ToggleControl toggleControl(final Object owner, final String beanPropertyName, final String caption,
                                            final EventObserver<Boolean> changeEvent) {
    return toggleControl(owner, beanPropertyName, caption, changeEvent, null);
  }

  /**
   * Creates a toggle control based on the boolean property {@code beanPropertyName} in the owner object
   * @param owner the owner object
   * @param beanPropertyName the name of the boolean bean property, must have a public setter and getter
   * @param caption the control caption
   * @param changeEvent an event fired each time the property value changes in the underlying object
   * @param enabledState the state which controls the enabled state of the control
   * @return a toggle control
   */
  public static ToggleControl toggleControl(final Object owner, final String beanPropertyName, final String caption,
                                            final EventObserver<Boolean> changeEvent, final StateObserver enabledState) {
    return toggleControl(owner, beanPropertyName, caption, changeEvent, enabledState, false);
  }

  /**
   * Creates a toggle control based on the boolean property {@code beanPropertyName} in the owner object
   * @param owner the owner object
   * @param beanPropertyName the name of the boolean bean property, must have a public setter and getter
   * @param caption the control caption
   * @param changeEvent an event fired each time the property value changes in the underlying object
   * @param enabledState the state which controls the enabled state of the control
   * @param tristate if true then a tristate (false, true, null) button model is used
   * @return a toggle control
   */
  public static ToggleControl toggleControl(final Object owner, final String beanPropertyName, final String caption,
                                            final EventObserver<Boolean> changeEvent, final StateObserver enabledState,
                                            final boolean tristate) {
    final ButtonModel buttonModel;
    if (tristate) {
      buttonModel = new TristateButtonModel();
    }
    else {
      buttonModel = new JToggleButton.ToggleButtonModel();
    }
    Values.link(Values.beanValue(owner, beanPropertyName, tristate ? Boolean.class : boolean.class, changeEvent), UiValues.booleanValue(buttonModel));

    return new ToggleControl(caption, buttonModel, enabledState);
  }

  /**
   * Creates a ToggleControl based on the given {@link State}
   * @param state the state to toggle
   * @return a ToggleControl based on the given state
   */
  public static ToggleControl toggleControl(final State state) {
    return toggleControl(state, null);
  }

  /**
   * Creates a ToggleControl based on the given {@link State}
   * @param state the state to toggle
   * @param name the name of this control
   * @return a ToggleControl based on the given state
   */
  public static ToggleControl toggleControl(final State state, final String name) {
    return toggleControl(state, name, (StateObserver) null);
  }

  /**
   * Creates a ToggleControl based on the given {@link State}
   * @param state the state to toggle
   * @param name the name of this control
   * @param enabledState the state which controls the enabled state of the control
   * @return a ToggleControl based on the given state
   */
  public static ToggleControl toggleControl(final State state, final String name, final StateObserver enabledState) {
    return toggleControl(state, name, enabledState, null);
  }

  /**
   * Creates a ToggleControl based on the given {@link State}
   * @param state the state to toggle
   * @param name the name of this control
   * @param enabledState the state which controls the enabled state of the control
   * @param icon the icon
   * @return a ToggleControl based on the given state
   */
  public static ToggleControl toggleControl(final State state, final String name, final StateObserver enabledState,
                                            final Icon icon) {
    return toggleControl(Values.stateValue(state), name, enabledState, icon);
  }

  /**
   * Creates a ToggleControl based on the given boolean {@link Value}.
   * If the value is nullable then a TristateButtonModel is used.
   * @param value the value to toggle
   * @return a ToggleControl based on the given state
   * @see Value#isNullable()
   */
  public static ToggleControl toggleControl(final Value<Boolean> value) {
    return toggleControl(value, null);
  }

  /**
   * Creates a ToggleControl based on the given boolean {@link Value}
   * If the value is nullable then a TristateButtonModel is used.
   * @param value the value to toggle
   * @param name the name of this control
   * @return a ToggleControl based on the given state
   * @see Value#isNullable()
   */
  public static ToggleControl toggleControl(final Value<Boolean> value, final String name) {
    return toggleControl(value, name, (StateObserver) null);
  }

  /**
   * Creates a ToggleControl based on the given boolean {@link Value}
   * If the value is nullable then a TristateButtonModel is used.
   * @param value the value to toggle
   * @param name the name of this control
   * @param enabledState the state which controls the enabled state of the control
   * @return a ToggleControl based on the given state
   * @see Value#isNullable()
   */
  public static ToggleControl toggleControl(final Value<Boolean> value, final String name, final StateObserver enabledState) {
    return toggleControl(value, name, enabledState, null);
  }

  /**
   * Creates a ToggleControl based on the given boolean {@link Value}
   * If the value is nullable then a TristateButtonModel is used.
   * @param value the value to toggle
   * @param name the name of this control
   * @param enabledState the state which controls the enabled state of the control
   * @param icon the icon
   * @return a ToggleControl based on the given state
   * @see Value#isNullable()
   */
  public static ToggleControl toggleControl(final Value<Boolean> value, final String name, final StateObserver enabledState,
                                            final Icon icon) {
    final ButtonModel buttonModel = value.isNullable() ? new TristateButtonModel() : new JToggleButton.ToggleButtonModel();
    Values.link(value, UiValues.booleanValue(buttonModel));

    final ToggleControl control = new ToggleControl(name, buttonModel, enabledState);
    control.setIcon(icon);

    return control;
  }

  /**
   * Creates a Control which fires the given event on action performed
   * @param event the event
   * @return a control which fires the given event
   */
  public static Control eventControl(final Event<ActionEvent> event) {
    return control(event::fire);
  }

  private static final class CommandControl extends Control {

    private final Control.Command command;

    private CommandControl(final Control.Command command, final String name, final StateObserver enabledObserver) {
      super(name, enabledObserver);
      this.command = Objects.requireNonNull(command);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      try {
        command.perform();
      }
      catch (final CancelException ce) {/*Operation cancelled*/}
      catch (final RuntimeException re) {
        throw re;
      }
      catch (final Exception ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  /**
   * A Control for toggling a button model
   */
  public static final class ToggleControl extends Control {

    private final ButtonModel buttonModel;

    /**
     * @param name the name
     * @param buttonModel the button model
     * @param enabledObserver an observer indicating when this control should be enabled
     */
    private ToggleControl(final String name, final ButtonModel buttonModel, final StateObserver enabledObserver) {
      super(name, enabledObserver);
      this.buttonModel = buttonModel;
      if (enabledObserver != null) {
        enabledObserver.addDataListener(buttonModel::setEnabled);
        buttonModel.setEnabled(enabledObserver.isActive());
      }
    }

    /**
     * @return the button model
     */
    public ButtonModel getButtonModel() {
      return buttonModel;
    }

    @Override
    protected ToggleControl doSetMnemonic(final int mnemonic) {
      this.buttonModel.setMnemonic(mnemonic);
      return (ToggleControl) super.doSetMnemonic(mnemonic);
    }
  }
}