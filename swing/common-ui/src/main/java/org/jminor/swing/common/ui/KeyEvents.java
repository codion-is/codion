/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.text.JTextComponent;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

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

  /**
   * Adds a key event to the component which transfers focus
   * on enter, and backwards if shift is down
   * @param component the component
   * @param <T> the component type
   * @see #removeTransferFocusOnEnter(JTextComponent)
   * @return the component
   */
  public static <T extends JComponent> T transferFocusOnEnter(final T component) {
    addKeyEvent(component, KeyEvent.VK_ENTER, 0, JComponent.WHEN_FOCUSED, false,
            new TransferFocusAction(component));
    addKeyEvent(component, KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK, JComponent.WHEN_FOCUSED, false,
            new TransferFocusAction(component, true));

    return component;
  }

  /**
   * Removes the transfer focus action added via {@link #transferFocusOnEnter(JComponent)}
   * @param component the component
   * @param <T> the component type
   * @return the component
   */
  public static <T extends JTextComponent> T removeTransferFocusOnEnter(final T component) {
    addKeyEvent(component, KeyEvent.VK_ENTER, 0, JComponent.WHEN_FOCUSED, false, null);
    addKeyEvent(component, KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK, JComponent.WHEN_FOCUSED, false, null);

    return component;
  }

  /**
   * Instantiates an Action for transferring keyboard focus forward.
   * @param component the component
   * @return an Action for transferring focus
   */
  public static Action transferFocusForwardAction(final JComponent component) {
    return new TransferFocusAction(component);
  }

  /**
   * Instantiates an Action for transferring keyboard focus backward.
   * @param component the component
   * @return an Action for transferring focus
   */
  public static Action transferFocusBackwardAction(final JComponent component) {
    return new TransferFocusAction(component, true);
  }

  /**
   * An action which transfers focus either forward or backward for a given component
   */
  private static final class TransferFocusAction extends AbstractAction {

    private final JComponent component;
    private final boolean backward;

    /**
     * Instantiates an Action for transferring keyboard focus.
     * @param component the component
     */
    private TransferFocusAction(final JComponent component) {
      this(component, false);
    }

    /**
     * @param component the component
     * @param backward if true the focus is transferred backward
     */
    private TransferFocusAction(final JComponent component, final boolean backward) {
      super(backward ? "KeyEvents.transferFocusBackward" : "KeyEvents.transferFocusForward");
      this.component = component;
      this.backward = backward;
    }

    /**
     * Transfers focus according the the value of {@code backward}
     * @param e the action event
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
      if (backward) {
        component.transferFocusBackward();
      }
      else {
        component.transferFocus();
      }
    }
  }
}
