/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui;

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
 * @see #builder()
 */
public final class KeyEvents {

  private KeyEvents() {}

  /**
   * Instantiates a new {@link KeyEventBuilder} instance.
   * @return a {@link KeyEventBuilder} instance.
   */
  public static KeyEventBuilder builder() {
    return new DefaultKeyEventBuilder();
  }

  /**
   * A Builder for adding a key event to a component, with a default onKeyRelease trigger
   * and condition {@link JComponent.WHEN_FOCUSED}.
   */
  public interface KeyEventBuilder {

    /**
     * @param keyEvent the key event
     * @return this builder instance
     */
    KeyEventBuilder keyEvent(int keyEvent);

    /**
     * @param modifiers the modifiers
     * @return this builder instance
     */
    KeyEventBuilder modifiers(int modifiers);

    /**
     * @param condition the condition
     * @return this builder instance
     */
    KeyEventBuilder condition(int condition);

    /**
     * @return this builder instance
     */
    KeyEventBuilder onKeyPressed();

    /**
     * @return this builder instance
     */
    KeyEventBuilder onKeyReleased();

    /**
     * @param action the action, if null then the action binding is removed
     * @return this builder instance
     */
    KeyEventBuilder action(Action action);

    /**
     * Builds the key event and enables it on the given component
     * @param component the component
     */
    void enable(JComponent component);

    /**
     * Disables this key event on the given component
     * @param component the component
     */
    void disable(JComponent component);
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
    builder().keyEvent(KeyEvent.VK_ENTER)
            .condition(JComponent.WHEN_FOCUSED)
            .onKeyPressed()
            .action(new TransferFocusAction(component))
            .enable(component);
    builder().keyEvent(KeyEvent.VK_ENTER)
            .modifiers(KeyEvent.SHIFT_DOWN_MASK)
            .condition(JComponent.WHEN_FOCUSED)
            .onKeyPressed()
            .action(new TransferFocusAction(component, true))
            .enable(component);

    return component;
  }

  /**
   * Removes the transfer focus action added via {@link #transferFocusOnEnter(JComponent)}
   * @param component the component
   * @param <T> the component type
   * @return the component
   */
  public static <T extends JTextComponent> T removeTransferFocusOnEnter(final T component) {
    builder().keyEvent(KeyEvent.VK_ENTER)
            .condition(JComponent.WHEN_FOCUSED)
            .onKeyPressed()
            .disable(component);
    builder().keyEvent(KeyEvent.VK_ENTER)
            .modifiers(KeyEvent.SHIFT_DOWN_MASK)
            .condition(JComponent.WHEN_FOCUSED)
            .onKeyPressed()
            .disable(component);

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

  private static final class DefaultKeyEventBuilder implements KeyEventBuilder {

    private int keyEvent;
    private int modifiers;
    private int condition = JComponent.WHEN_FOCUSED;
    private boolean onKeyRelease = true;
    private Action action;

    @Override
    public KeyEventBuilder keyEvent(final int keyEvent) {
      this.keyEvent = keyEvent;
      return this;
    }

    @Override
    public KeyEventBuilder modifiers(final int modifiers) {
      this.modifiers = modifiers;
      return this;
    }

    @Override
    public KeyEventBuilder condition(final int condition) {
      this.condition = condition;
      return this;
    }

    @Override
    public KeyEventBuilder onKeyPressed() {
      this.onKeyRelease = false;
      return this;
    }

    @Override
    public KeyEventBuilder onKeyReleased() {
      this.onKeyRelease = true;
      return this;
    }

    @Override
    public KeyEventBuilder action(final Action action) {
      this.action = action;
      return this;
    }

    @Override
    public void enable(final JComponent component) {
      requireNonNull(component, "component");
      if (action == null) {
        throw new IllegalStateException("Can not enable a key event without an action");
      }
      Object actionName = action.getValue(Action.NAME);
      if (actionName == null) {
        actionName = createDefaultActionName(component);
      }
      component.getActionMap().put(actionName, action);
      component.getInputMap(condition).put(KeyStroke.getKeyStroke(keyEvent, modifiers, onKeyRelease), actionName);
    }

    @Override
    public void disable(final JComponent component) {
      requireNonNull(component, "component");
      if (action == null) {
        throw new IllegalStateException("Can not disable a key event without an action");
      }
      Object actionName = action.getValue(Action.NAME);
      if (actionName == null) {
        actionName = createDefaultActionName(component);
      }
      component.getActionMap().put(actionName, null);
      component.getInputMap(condition).put(KeyStroke.getKeyStroke(keyEvent, modifiers, onKeyRelease), null);
    }

    private String createDefaultActionName(final JComponent component) {
      return component.getClass().getSimpleName() + keyEvent + modifiers + (onKeyRelease ? "keyReleased" : "keyPressed");
    }
  }
}
