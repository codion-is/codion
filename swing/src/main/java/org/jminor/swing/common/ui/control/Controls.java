/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.jminor.common.Event;
import org.jminor.common.EventObserver;
import org.jminor.common.Events;
import org.jminor.common.State;
import org.jminor.common.StateObserver;
import org.jminor.common.Value;
import org.jminor.common.Values;
import org.jminor.common.model.CancelException;
import org.jminor.swing.common.model.checkbox.TristateButtonModel;

import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
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
   * @param icon the icon
   * @return a Control for calling the given {@link Control.Command}
   */
  public static Control commandControl(final Control.Command command, final Icon icon) {
    return commandControl(command, null, null, null, -1, null, icon);
  }

  /**
   * Creates a control based on a {@link Control.Command}
   * @param command the {@link Control.Command} on which to base this control
   * @param name the name of the control
   * @return a Control for calling the given {@link Control.Command}
   */
  public static Control commandControl(final Control.Command command, final String name) {
    return commandControl(command, name, null);
  }

  /**
   * Creates a control based on a {@link Control.Command}
   * @param command the {@link Control.Command} on which to base this control
   * @param name the name of the control
   * @param enabledState the state which controls the enabled state of the control
   * @return a Control for calling the given {@link Control.Command}
   */
  public static Control commandControl(final Control.Command command, final String name, final StateObserver enabledState) {
    return new ActionControl(command, name, enabledState);
  }

  /**
   * Creates a control based on a {@link Control.Command}
   * @param command the {@link Control.Command} on which to base this control
   * @param name the name of the control
   * @param enabledState the state which controls the enabled state of the control
   * @param description a string describing the control
   * @return a Control for calling the given {@link Control.Command}
   */
  public static Control commandControl(final Control.Command command, final String name, final StateObserver enabledState,
                                       final String description) {
    return commandControl(command, name, enabledState).setDescription(description);
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
  public static Control commandControl(final Control.Command command, final String name, final StateObserver enabledState,
                                       final String description, final int mnemonic) {
    return commandControl(command, name, enabledState, description).setMnemonic(mnemonic);
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
  public static Control commandControl(final Control.Command command, final String name, final StateObserver enabledState,
                                       final String description, final int mnemonic, final KeyStroke keyStroke) {
    return commandControl(command, name, enabledState, description, mnemonic).setKeyStroke(keyStroke);
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
  public static Control commandControl(final Control.Command command, final String name, final StateObserver enabledState,
                                       final String description, final int mnemonic, final KeyStroke keyStroke, final Icon icon) {
    return commandControl(command, name, enabledState, description, mnemonic, keyStroke).setIcon(icon);
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
   * @param tristate if truen then a tristate (false, true, null) model is used
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
    Values.link(Values.beanValue(owner, beanPropertyName, tristate ? Boolean.class : boolean.class, changeEvent), new BooleanValue(buttonModel));

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
   * @param enabledState the state which controls the enabled state of the control
   * @return a ToggleControl based on the given state
   */
  public static ToggleControl toggleControl(final State state, final StateObserver enabledState) {
    final ButtonModel buttonModel = new JToggleButton.ToggleButtonModel();
    Values.link(Values.stateValue(state), new BooleanValue(buttonModel));

    return new ToggleControl("toggleControl", buttonModel, enabledState);
  }

  /**
   * Creates a Control which fires the given event on action performed
   * @param event the event
   * @return a control for firing the given event
   */
  public static Control eventControl(final Event<ActionEvent> event) {
    return new Control() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        event.fire(e);
      }
    };
  }

  private static final class ActionControl extends Control {
    private final Control.Command command;

    private ActionControl(final Control.Command command, final String name, final StateObserver enabledObserver) {
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
   * So as to not introduce a dependency to common.ui
   */
  private static final class BooleanValue implements Value<Boolean> {
    private final ButtonModel buttonModel;
    private final Event<Boolean> changeEvent = Events.event();

    private BooleanValue(final ButtonModel buttonModel) {
      this.buttonModel = buttonModel;
      buttonModel.addItemListener(e -> changeEvent.fire());
    }

    @Override
    public Boolean get() {
      if (buttonModel instanceof TristateButtonModel && ((TristateButtonModel) buttonModel).isIndeterminate()) {
        return null;
      }

      return buttonModel.isSelected();
    }

    @Override
    public void set(final Boolean value) {
      if (SwingUtilities.isEventDispatchThread()) {
        setValue(value);
      }
      else {
        try {
          SwingUtilities.invokeAndWait(() -> setValue(value));
        }
        catch (final Exception e) {
          throw new RuntimeException(e);
        }
      }
    }

    private void setValue(final Boolean value) {
      if (value == null && buttonModel instanceof TristateButtonModel) {
        ((TristateButtonModel) buttonModel).setIndeterminate();
      }
      else {
        buttonModel.setSelected(value != null && value);
      }
    }

    @Override
    public EventObserver<Boolean> getObserver() {
      return changeEvent.getObserver();
    }
  }
}