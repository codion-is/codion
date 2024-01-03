/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.key;

import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import static java.awt.event.KeyEvent.VK_UNDEFINED;
import static java.util.Objects.requireNonNull;
import static javax.swing.JComponent.WHEN_FOCUSED;
import static javax.swing.KeyStroke.getKeyStroke;

/**
 * A factory for key event builders.
 * <pre>
 * JTextField textField = new JTextField();
 *
 * KeyEvents.builder(VK_DOWN)
 *          .onKeyRelease(false)
 *          .modifiers(CTRL_DOWN_MASK)
 *          .condition(WHEN_FOCUSED)
 *          .action(new FindNextAction())
 *          .enable(textField);
 * </pre>
 * @see #builder()
 * @see #builder(int)
 * @see #builder(KeyStroke)
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
   * @param keyCode the key code
   * @return a {@link Builder} instance.
   */
  public static Builder builder(int keyCode) {
    return new DefaultBuilder()
            .keyCode(keyCode);
  }

  /**
   * Instantiates a new {@link KeyEvents.Builder} instance.
   * Note that an Action must be set via {@link Builder#action(Action)} before enabling/disabling.
   * @param keyStroke the key stroke
   * @return a {@link Builder} instance.
   */
  public static Builder builder(KeyStroke keyStroke) {
    return new DefaultBuilder(keyStroke);
  }

  /**
   * A Builder for adding a key event to a component, with a default onKeyRelease trigger
   * and condition {@link JComponent#WHEN_FOCUSED}.
   * @see KeyEvents#builder(int)
   */
  public interface Builder {

    /**
     * @param keyCode the key code
     * @return this builder instance
     */
    Builder keyCode(int keyCode);

    /**
     * @param keyChar the key char
     * @return this builder instance
     */
    Builder keyChar(char keyChar);

    /**
     * @param modifiers the modifiers
     * @return this builder instance
     */
    Builder modifiers(int modifiers);

    /**
     * Default false.
     * @param onKeyRelease true if on key release
     * @return this builder instance
     */
    Builder onKeyRelease(boolean onKeyRelease);

    /**
     * @param keyStroke the key stroke
     * @return this builder instance
     */
    Builder keyStroke(KeyStroke keyStroke);

    /**
     * Sets the key event condition, {@link JComponent#WHEN_FOCUSED} by default.
     * @param condition the condition
     * @return this builder instance
     */
    Builder condition(int condition);

    /**
     * @param action the action, if null then the action binding is removed
     * @return this builder instance
     */
    Builder action(Action action);

    /**
     * Builds the key event and enables it on the given components
     * @param components the components
     * @return this builder instance
     * @throws IllegalStateException in case no action has been set
     */
    Builder enable(JComponent... components);

    /**
     * Disables this key event on the given components
     * @param components the components
     * @return this builder instance
     * @throws IllegalStateException in case no action has been set
     */
    Builder disable(JComponent... components);
  }

  private static final class DefaultBuilder implements Builder {

    private KeyStroke keyStroke;
    private int condition = WHEN_FOCUSED;
    private Action action;

    private DefaultBuilder() {
      this(getKeyStroke(VK_UNDEFINED, 0, false));
    }

    private DefaultBuilder(KeyStroke keyStroke) {
      this.keyStroke = requireNonNull(keyStroke);
    }

    @Override
    public Builder keyCode(int keyCode) {
      this.keyStroke = getKeyStroke(keyCode, keyStroke.getModifiers(), keyStroke.isOnKeyRelease());
      return this;
    }

    @Override
    public Builder keyChar(char keyChar) {
      this.keyStroke = getKeyStroke(keyChar, keyStroke.getModifiers(), keyStroke.isOnKeyRelease());
      return this;
    }

    @Override
    public Builder modifiers(int modifiers) {
      this.keyStroke = getKeyStroke(keyStroke.getKeyCode(), modifiers, keyStroke.isOnKeyRelease());
      return this;
    }

    @Override
    public Builder onKeyRelease(boolean onKeyRelease) {
      this.keyStroke = getKeyStroke(keyStroke.getKeyCode(), keyStroke.getModifiers(), onKeyRelease);
      return this;
    }

    @Override
    public Builder keyStroke(KeyStroke keyStroke) {
      this.keyStroke = requireNonNull(keyStroke);
      return this;
    }

    @Override
    public Builder condition(int condition) {
      this.condition = condition;
      return this;
    }

    @Override
    public Builder action(Action action) {
      this.action = requireNonNull(action);
      return this;
    }

    @Override
    public Builder enable(JComponent... components) {
      for (JComponent component : requireNonNull(components)) {
        enable(requireNonNull(component), actionMapKey(component));
      }

      return this;
    }

    @Override
    public Builder disable(JComponent... components) {
      for (JComponent component : requireNonNull(components)) {
        disable(requireNonNull(component), actionMapKey(component));
      }

      return this;
    }

    private Object actionMapKey(JComponent component) {
      if (action == null) {
        throw new IllegalStateException("Unable to enable/disable a key event without an associated action");
      }
      Object actionMapKey = action.getValue(Action.NAME);
      if (actionMapKey == null) {
        actionMapKey = createDefaultActionMapKey(component);
      }

      return actionMapKey;
    }

    private String createDefaultActionMapKey(JComponent component) {
      return new StringBuilder(component.getClass().getSimpleName())
              .append(" ").append(keyStroke.toString())
              .append(" ").append(condition)
              .toString();
    }

    private void enable(JComponent component, Object actionMapKey) {
      component.getActionMap().put(actionMapKey, action);
      component.getInputMap(condition).put(keyStroke, actionMapKey);
      if (component instanceof JComboBox<?>) {
        enable((JComponent) ((JComboBox<?>) component).getEditor().getEditorComponent(), actionMapKey);
      }
    }

    private void disable(JComponent component, Object actionMapKey) {
      component.getActionMap().put(actionMapKey, null);
      component.getInputMap(condition).put(keyStroke, null);
      if (component instanceof JComboBox<?>) {
        disable((JComponent) ((JComboBox<?>) component).getEditor().getEditorComponent(), actionMapKey);
      }
    }
  }
}
