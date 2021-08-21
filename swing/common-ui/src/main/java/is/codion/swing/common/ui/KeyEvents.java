/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import static java.util.Objects.requireNonNull;

/**
 * A factory for key event builders.
 * <pre>
 * JTextField textField = new JTextField();

 * KeyEvents.builder(VK_UP)
 *          .onKeyPressed()
 *          .modifiers(ALT_DOWN_MASK | CTRL_DOWN_MASK)
 *          .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
 *          .action(new NavigateUpAction())
 *          .enable(textField);
 * </pre>
 * @see #builder(int)
 */
public final class KeyEvents {

  private KeyEvents() {}

  /**
   * Instantiates a new {@link KeyEventBuilder} instance.
   * @param keyEvent the key event
   * @return a {@link KeyEventBuilder} instance.
   */
  public static KeyEventBuilder builder(final int keyEvent) {
    return new DefaultKeyEventBuilder(keyEvent);
  }

  /**
   * A Builder for adding a key event to a component, with a default onKeyRelease trigger
   * and condition {@link JComponent#WHEN_FOCUSED}.
   * @see KeyEvents#builder(int)
   */
  public interface KeyEventBuilder {

    /**
     * @param modifiers the modifiers
     * @return this builder instance
     */
    KeyEventBuilder modifiers(int modifiers);

    /**
     * Sets the key event condition, {@link JComponent#WHEN_FOCUSED} by default.
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
     * Returns a KeyStroke based on this builder
     * @return a KeyStroke
     */
    KeyStroke getKeyStroke();

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

    private final int keyEvent;

    private int modifiers;
    private int condition = JComponent.WHEN_FOCUSED;
    private boolean onKeyRelease = true;
    private Action action;

    private DefaultKeyEventBuilder(final int keyEvent) {
      this.keyEvent = keyEvent;
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
    public KeyStroke getKeyStroke() {
      return KeyStroke.getKeyStroke(keyEvent, modifiers, onKeyRelease);
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
      component.getInputMap(condition).put(getKeyStroke(), actionName);
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
      component.getInputMap(condition).put(getKeyStroke(), null);
    }

    private String createDefaultActionName(final JComponent component) {
      return component.getClass().getSimpleName() + keyEvent + modifiers + (onKeyRelease ? "keyReleased" : "keyPressed");
    }
  }
}
