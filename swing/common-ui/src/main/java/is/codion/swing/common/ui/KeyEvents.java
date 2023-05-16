/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui;

import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import static java.util.Objects.requireNonNull;
import static javax.swing.JComponent.WHEN_FOCUSED;
import static javax.swing.KeyStroke.getKeyStroke;

/**
 * A factory for key event builders.
 * <pre>
 * JTextField textField = new JTextField();
 *
 * KeyEvents.builder(VK_UP)
 *          .onKeyPressed()
 *          .modifiers(ALT_DOWN_MASK | CTRL_DOWN_MASK)
 *          .condition(WHEN_FOCUSED)
 *          .action(new NavigateUpAction())
 *          .enable(textField);
 * </pre>
 * @see #builder(int)
 */
public final class KeyEvents {

  private KeyEvents() {}

  /**
   * Instantiates a new {@link KeyEvents.Builder} instance.
   * Note that an Action must be set via {@link Builder#action(Action)} before enabling/disabling.
   * @return a {@link Builder} instance.
   */
  public static Builder builder() {
    return new DefaultBuilder();
  }

  /**
   * Instantiates a new {@link KeyEvents.Builder} instance.
   * Note that an Action must be set via {@link Builder#action(Action)} before enabling/disabling.
   * @param keyEvent the key event
   * @return a {@link Builder} instance.
   */
  public static Builder builder(int keyEvent) {
    return new DefaultBuilder()
            .keyEvent(keyEvent);
  }

  /**
   * A Builder for adding a key event to a component, with a default onKeyRelease trigger
   * and condition {@link JComponent#WHEN_FOCUSED}.
   * @see KeyEvents#builder(int)
   */
  public interface Builder {

    /**
     * @param keyEvent the key event code
     * @return this builder instance
     */
    Builder keyEvent(int keyEvent);

    /**
     * @param modifiers the modifiers
     * @return this builder instance
     */
    Builder modifiers(int modifiers);

    /**
     * Sets the key event condition, {@link JComponent#WHEN_FOCUSED} by default.
     * @param condition the condition
     * @return this builder instance
     */
    Builder condition(int condition);

    /**
     * @return this builder instance
     */
    Builder onKeyPressed();

    /**
     * @return this builder instance
     */
    Builder onKeyReleased();

    /**
     * @param action the action, if null then the action binding is removed
     * @return this builder instance
     */
    Builder action(Action action);

    /**
     * Returns a KeyStroke based on this builder
     * @return a KeyStroke
     */
    KeyStroke keyStroke();

    /**
     * Builds the key event and enables it on the given component
     * @param component the component
     * @return this builder instance
     * @throws IllegalStateException in case no action has been set
     */
    Builder enable(JComponent component);

    /**
     * Disables this key event on the given component
     * @param component the component
     * @return this builder instance
     * @throws IllegalStateException in case no action has been set
     */
    Builder disable(JComponent component);
  }

  private static final class DefaultBuilder implements Builder {

    private int keyEvent;
    private int modifiers;
    private int condition = WHEN_FOCUSED;
    private boolean onKeyReleased = false;
    private Action action;

    @Override
    public Builder keyEvent(int keyEvent) {
      this.keyEvent = keyEvent;
      return this;
    }

    @Override
    public Builder modifiers(int modifiers) {
      this.modifiers = modifiers;
      return this;
    }

    @Override
    public Builder condition(int condition) {
      this.condition = condition;
      return this;
    }

    @Override
    public Builder onKeyPressed() {
      this.onKeyReleased = false;
      return this;
    }

    @Override
    public Builder onKeyReleased() {
      this.onKeyReleased = true;
      return this;
    }

    @Override
    public Builder action(Action action) {
      this.action = action;
      return this;
    }

    @Override
    public KeyStroke keyStroke() {
      return getKeyStroke(keyEvent, modifiers, onKeyReleased);
    }

    @Override
    public Builder enable(JComponent component) {
      return enable(requireNonNull(component), keyStroke(), actionMapKey(component));
    }

    @Override
    public Builder disable(JComponent component) {
      return disable(requireNonNull(component), keyStroke(), actionMapKey(component));
    }

    private Object actionMapKey(JComponent component) {
      if (action == null) {
        throw new IllegalStateException("Can not enable/disable a key event without an action");
      }
      Object actionMapKey = action.getValue(Action.NAME);
      if (actionMapKey == null) {
        actionMapKey = createDefaultActionMapKey(component);
      }

      return actionMapKey;
    }

    private String createDefaultActionMapKey(JComponent component) {
      return new StringBuilder(component.getClass().getSimpleName())
              .append("_k:").append(keyEvent)
              .append("_m:").append(modifiers)
              .append("_c:").append(condition)
              .append(onKeyReleased ? "_released" : "_pressed")
              .toString();
    }

    private Builder enable(JComponent component, KeyStroke keyStroke, Object actionMapKey) {
      component.getActionMap().put(actionMapKey, action);
      component.getInputMap(condition).put(keyStroke, actionMapKey);
      if (component instanceof JComboBox<?>) {
        enable((JComponent) ((JComboBox<?>) component).getEditor().getEditorComponent(), keyStroke, actionMapKey);
      }

      return this;
    }

    private Builder disable(JComponent component, KeyStroke keyStroke, Object actionMapKey) {
      component.getActionMap().put(actionMapKey, null);
      component.getInputMap(condition).put(keyStroke, null);
      if (component instanceof JComboBox<?>) {
        disable((JComponent) ((JComboBox<?>) component).getEditor().getEditorComponent(), keyStroke, actionMapKey);
      }

      return this;
    }
  }
}
