/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui;

import javax.swing.Action;
import javax.swing.JComboBox;
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
   * Instantiates a new {@link KeyEvents.Builder} instance.
   * @return a {@link Builder} instance.
   */
  public static Builder builder() {
    return new DefaultBuilder();
  }

  /**
   * Instantiates a new {@link KeyEvents.Builder} instance.
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
     */
    Builder enable(JComponent component);

    /**
     * Disables this key event on the given component
     * @param component the component
     * @return this builder instance
     */
    Builder disable(JComponent component);
  }

  private static final class DefaultBuilder implements Builder {

    private int keyEvent;
    private int modifiers;
    private int condition = JComponent.WHEN_FOCUSED;
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
      return KeyStroke.getKeyStroke(keyEvent, modifiers, onKeyReleased);
    }

    @Override
    public Builder enable(JComponent component) {
      requireNonNull(component, "component");
      if (action == null) {
        throw new IllegalStateException("Can not enable a key event without an action");
      }
      Object actionName = action.getValue(Action.NAME);
      if (actionName == null) {
        actionName = createDefaultActionName(component);
      }
      KeyStroke keyStroke = keyStroke();
      enable(component, action, condition, keyStroke, actionName);
      if (component instanceof JComboBox<?>) {
        JComponent editorComponent = (JComponent) ((JComboBox<?>) component).getEditor().getEditorComponent();
        enable(editorComponent, action, condition, keyStroke, actionName);
      }

      return this;
    }

    @Override
    public Builder disable(JComponent component) {
      requireNonNull(component, "component");
      if (action == null) {
        throw new IllegalStateException("Can not disable a key event without an action");
      }
      Object actionName = action.getValue(Action.NAME);
      if (actionName == null) {
        actionName = createDefaultActionName(component);
      }
      KeyStroke keyStroke = keyStroke();
      disable(component, condition, keyStroke, actionName);
      if (component instanceof JComboBox<?>) {
        JComponent editorComponent = (JComponent) ((JComboBox<?>) component).getEditor().getEditorComponent();
        disable(editorComponent, condition, keyStroke, actionName);
      }

      return this;
    }

    private String createDefaultActionName(JComponent component) {
      return component.getClass().getSimpleName() + keyEvent + modifiers + (onKeyReleased ? "keyReleased" : "keyPressed");
    }

    private static void enable(JComponent component, Action action, int condition, KeyStroke keyStroke, Object actionName) {
      component.getActionMap().put(actionName, action);
      component.getInputMap(condition).put(keyStroke, actionName);
    }

    private static void disable(JComponent component, int condition, KeyStroke keyStroke, Object actionName) {
      component.getActionMap().put(actionName, null);
      component.getInputMap(condition).put(keyStroke, null);
    }
  }
}
