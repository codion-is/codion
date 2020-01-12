/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import static java.util.Objects.requireNonNull;

/**
 * A utility class for key events.
 */
public final class KeyEvents {

  private KeyEvents() {}

  /**
   * Links the given action to the given key event on the given component via inputMap/actionMap, using the name
   * of the action as key for the actionMap, JComponent.WHEN_FOCUSED as condition, 0 as modifier and true for onKeyRelease.
   * @param component the component
   * @param keyEvent the key event
   * @param action the action
   * @throws NullPointerException in case {@code component}, {@code action} or the action name is null
   * @see KeyStroke#getKeyStroke(int, int, boolean)
   */
  public static void addKeyEvent(final JComponent component, final int keyEvent, final Action action) {
    addKeyEvent(component, keyEvent, 0, action);
  }

  /**
   * Links the given action to the given key event on the given component via inputMap/actionMap, using the name
   * of the action as key for the actionMap, JComponent.WHEN_FOCUSED as condition and true for onKeyRelease.
   * @param component the component
   * @param keyEvent the key event
   * @param modifiers the modifiers
   * @param action the action
   * @throws NullPointerException in case {@code component}, {@code action} or the action name is null
   * @see KeyStroke#getKeyStroke(int, int, boolean)
   */
  public static void addKeyEvent(final JComponent component, final int keyEvent, final int modifiers,
                                 final Action action) {
    addKeyEvent(component, keyEvent, modifiers, JComponent.WHEN_FOCUSED, action);
  }

  /**
   * Links the given action to the given key event on the given component via inputMap/actionMap, using the name
   * of the action as key for the actionMap and true for onKeyRelease.
   * @param component the component
   * @param keyEvent the key event
   * @param modifiers the modifiers
   * @param condition the condition
   * @param action the action
   * @throws NullPointerException in case {@code component}, {@code action} or the action name is null
   * @see KeyStroke#getKeyStroke(int, int, boolean)
   */
  public static void addKeyEvent(final JComponent component, final int keyEvent, final int modifiers, final int condition,
                                 final Action action) {
    addKeyEvent(component, keyEvent, modifiers, condition, true, action);
  }

  /**
   * Links the given action to the given key event on the given component via inputMap/actionMap, using the name
   * of the action as key for the actionMap, if {@code action} is null the binding is removed
   * @param component the component
   * @param keyEvent the key event
   * @param modifiers the modifiers
   * @param condition the condition
   * @param onKeyRelease the onKeyRelease condition
   * @param action the action, if null then the action binding is removed
   * @see KeyStroke#getKeyStroke(int, int, boolean)
   */
  public static void addKeyEvent(final JComponent component, final int keyEvent, final int modifiers, final int condition,
                                 final boolean onKeyRelease, final Action action) {
    requireNonNull(component, "component");
    Object actionName = null;
    if (action != null) {
      actionName = action.getValue(Action.NAME);
      if (actionName == null) {
        actionName = component.getClass().getSimpleName() + keyEvent + modifiers + onKeyRelease;
      }
      component.getActionMap().put(actionName, action);
    }
    component.getInputMap(condition).put(KeyStroke.getKeyStroke(keyEvent, modifiers, onKeyRelease), actionName);
  }
}
