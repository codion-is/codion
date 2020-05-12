/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.jminor.common.event.Event;
import org.jminor.common.event.EventObserver;
import org.jminor.common.state.State;
import org.jminor.common.state.StateObserver;
import org.jminor.common.value.Nullable;
import org.jminor.common.value.Value;
import org.jminor.common.value.Values;

import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;

import static org.jminor.common.value.Values.propertyValue;

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
   * @param enabledState the state which controls the enabled state of the control
   * @return a Control for calling the given {@link Control.Command}
   */
  public static Control control(final Control.Command command, final StateObserver enabledState) {
    return control(command, (String) null, enabledState);
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
   * @param changeEvent an event triggered each time the property value changes in the underlying object
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
   * @param changeEvent an event triggered each time the property value changes in the underlying object
   * @param enabledState the state which controls the enabled state of the control
   * @return a toggle control
   */
  public static ToggleControl toggleControl(final Object owner, final String beanPropertyName, final String caption,
                                            final EventObserver<Boolean> changeEvent, final StateObserver enabledState) {
    return toggleControl(owner, beanPropertyName, caption, changeEvent, enabledState, Nullable.NO);
  }

  /**
   * Creates a toggle control based on the boolean property {@code beanPropertyName} in the owner object
   * @param owner the owner object
   * @param beanPropertyName the name of the boolean bean property, must have a public setter and getter
   * @param caption the control caption
   * @param changeEvent an event triggered each time the property value changes in the underlying object
   * @param enabledState the state which controls the enabled state of the control
   * @param nullable if yes then a nullable (false, true, null) button model is used
   * @return a toggle control
   */
  public static ToggleControl toggleControl(final Object owner, final String beanPropertyName, final String caption,
                                            final EventObserver<Boolean> changeEvent, final StateObserver enabledState,
                                            final Nullable nullable) {
    return new DefaultToggleControl(caption, propertyValue(owner, beanPropertyName,
            nullable == Nullable.YES ? Boolean.class : boolean.class, changeEvent), enabledState);
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
   * @param value the value to toggle
   * @return a ToggleControl based on the given state
   * @see Value#isNullable()
   */
  public static ToggleControl toggleControl(final Value<Boolean> value) {
    return toggleControl(value, null);
  }

  /**
   * Creates a ToggleControl based on the given boolean {@link Value}.
   * @param value the value to toggle
   * @param name the name of this control
   * @return a ToggleControl based on the given state
   * @see Value#isNullable()
   */
  public static ToggleControl toggleControl(final Value<Boolean> value, final String name) {
    return toggleControl(value, name, (StateObserver) null);
  }

  /**
   * Creates a ToggleControl based on the given boolean {@link Value}.
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
   * Creates a ToggleControl based on the given boolean {@link Value}.
   * @param value the value to toggle
   * @param name the name of this control
   * @param enabledState the state which controls the enabled state of the control
   * @param icon the icon
   * @return a ToggleControl based on the given state
   * @see Value#isNullable()
   */
  public static ToggleControl toggleControl(final Value<Boolean> value, final String name, final StateObserver enabledState,
                                            final Icon icon) {
    return (ToggleControl) new DefaultToggleControl(name, value, enabledState).setIcon(icon);
  }

  /**
   * Creates a Control which triggers the given event on action performed
   * @param event the event
   * @return a control which triggers the given event
   */
  public static Control eventControl(final Event<ActionEvent> event) {
    return control(event::onEvent);
  }

  /**
   * Constructs a new ControlList.
   * @return a new ControlList instance.
   */
  public static ControlList controlList() {
    return controlList("");
  }

  /**
   * Constructs a new ControlList
   * @param name the control list name
   * @return a new ControlList instance.
   */
  public static ControlList controlList(final String name) {
    return controlList(name, (char) -1);
  }

  /**
   * Constructs a new ControlList
   * @param name the control list name
   * @param mnemonic the mnemonic to assign to this control list
   * @return a new ControlList instance.
   */
  public static ControlList controlList(final String name, final char mnemonic) {
    return controlList(name, mnemonic, (Icon) null);
  }

  /**
   * Constructs a new ControlList
   * @param name the control list name
   * @param mnemonic the mnemonic to assign to this control list
   * @param icon the icon
   * @return a new ControlList instance.
   */
  public static ControlList controlList(final String name, final char mnemonic, final Icon icon) {
    return controlList(name, mnemonic, icon, null);
  }

  /**
   * Constructs a new ControlList
   * @param name the control list name
   * @param mnemonic the mnemonic to assign to this control list
   * @param icon the icon
   * @param enabledState the state observer dictating the enable state of this control
   * @return a new ControlList instance.
   */
  public static ControlList controlList(final String name, final char mnemonic, final Icon icon, final StateObserver enabledState) {
    return new DefaultControlList(name, mnemonic, icon, enabledState);
  }

  /**
   * Constructs a new ControlList
   * @param controls the controls to add to this set
   * @return a new ControlList instance.
   */
  public static ControlList controlList(final Control... controls) {
    return controlList(null, controls);
  }

  /**
   * Constructs a new ControlList
   * @param name the control list name
   * @param controls the controls to add to this list
   * @return a new ControlList instance.
   */
  public static ControlList controlList(final String name, final Control... controls) {
    return controlList(name, (char) -1, controls);
  }

  /**
   * Constructs a new ControlList
   * @param name the control list name
   * @param mnemonic the mnemonic to assign to this control list
   * @param controls the controls to add to this list
   * @return a new ControlList instance.
   */
  public static ControlList controlList(final String name, final char mnemonic, final Control... controls) {
    return controlList(name, mnemonic, null, controls);
  }

  /**
   * Constructs a new ControlList
   * @param name the control list name
   * @param mnemonic the mnemonic to assign to this control list
   * @param enabledState the state observer dictating the enable state of this control
   * @param controls the controls to add to this list
   * @return a new ControlList instance.
   */
  public static ControlList controlList(final String name, final char mnemonic, final State enabledState, final Control... controls) {
    return controlList(name, mnemonic, enabledState, null, controls);
  }

  /**
   * Constructs a new ControlList
   * @param name the control list name
   * @param mnemonic the mnemonic to assign to this control list
   * @param enabledState the state observer dictating the enable state of this control
   * @param icon the icon
   * @param controls the controls to add to this list
   * @return a new ControlList instance.
   */
  public static ControlList controlList(final String name, final char mnemonic, final State enabledState,
                                        final Icon icon, final Control... controls) {
    return new DefaultControlList(name, mnemonic, enabledState, icon, controls);
  }
}