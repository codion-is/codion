/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

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
